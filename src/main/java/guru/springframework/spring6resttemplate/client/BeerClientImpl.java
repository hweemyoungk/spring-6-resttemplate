package guru.springframework.spring6resttemplate.client;

import guru.springframework.spring6resttemplate.model.BeerDTO;
import guru.springframework.spring6resttemplate.model.BeerDtoPageImpl;
import guru.springframework.spring6resttemplate.model.BeerStyle;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class BeerClientImpl implements BeerClient {
    // Autowires bean from RestTemplateBuilderConfig
    private final RestTemplateBuilder restTemplateBuilder;
    static final String GET_BEER_PATH = "/api/v1/beer";
    static final String GET_BEER_BY_ID_PATH = "/api/v1/beer/{beerId}";

    @Override
    public Page<BeerDTO> listBeers(
            String beerName,
            BeerStyle beerStyle,
            Boolean showInventory,
            Integer pageNumber,
            Integer pageSize
    ) {
        RestTemplate restTemplate = restTemplateBuilder.build();

        UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder
                .fromPath(GET_BEER_PATH);
        addQueryParamIfNotNull(uriComponentsBuilder, "beerName", beerName);
        addQueryParamIfNotNull(uriComponentsBuilder, "beerStyle", beerStyle);
        addQueryParamIfNotNull(uriComponentsBuilder, "showInventory", showInventory);
        addQueryParamIfNotNull(uriComponentsBuilder, "pageNumber", pageNumber);
        addQueryParamIfNotNull(uriComponentsBuilder, "pageSize", pageSize);

        /*ResponseEntity<String> stringResponse = restTemplate.getForEntity(
                GET_BEER_PATH,
                String.class
        );
        ResponseEntity<Map> mapResponse = restTemplate.getForEntity(
                BASE_URL + GET_BEER_PATH,
                Map.class
        );
        ResponseEntity<JsonNode> jsonNodeResponse = restTemplate.getForEntity(
                BASE_URL + GET_BEER_PATH,
                JsonNode.class
        );
        jsonNodeResponse.getBody()
                .findPath("content")
                .elements()
                .forEachRemaining(jsonNode -> {
                    System.out.println(jsonNode.get("beerName").asText());
                });*/

        ResponseEntity<BeerDtoPageImpl> pageResponse = restTemplate.getForEntity(
                uriComponentsBuilder.toUriString(),
                BeerDtoPageImpl.class
        );
        return pageResponse.getBody();
    }

    @Override
    public Page<BeerDTO> listBeers() {
        /*RestTemplate restTemplate = restTemplateBuilder.build();
        ResponseEntity<BeerDtoPageImpl> pageResponse = restTemplate.getForEntity(
                GET_BEER_PATH,
                BeerDtoPageImpl.class
        );
        return pageResponse.getBody();*/
        return listBeers(null, null, null, null, null);
    }

    @Override
    public void addQueryParamIfNotNull(UriComponentsBuilder uriComponentsBuilder, String name, Object value) {
        if (StringUtils.hasText(name) && value != null) {
            uriComponentsBuilder.queryParam(name, value);
        }
    }

    @Override
    public BeerDTO getBeerById(UUID beerId) {
        RestTemplate restTemplate = restTemplateBuilder.build();
        return restTemplate.getForObject(
                GET_BEER_BY_ID_PATH,
                BeerDTO.class,
                beerId
        );
    }

    @Override
    public BeerDTO createBeer(BeerDTO newDto) {
        RestTemplate restTemplate = restTemplateBuilder.build();
/*
        ResponseEntity<BeerDTO> response = restTemplate.postForEntity(
                GET_BEER_PATH,
                newDto,
                BeerDTO.class
        );
*/
        URI uri = restTemplate.postForLocation(GET_BEER_PATH, newDto);
        return restTemplate.getForObject(uri.getPath(), BeerDTO.class);
    }

    @Override
    public BeerDTO updateBeer(BeerDTO beerDTO) {
        RestTemplate restTemplate = restTemplateBuilder.build();
        restTemplate.put(
                GET_BEER_BY_ID_PATH,
                beerDTO,
                beerDTO.getId()
        );
        return getBeerById(beerDTO.getId());
    }

    @Override
    public void deleteBeer(UUID id) {
        RestTemplate restTemplate = restTemplateBuilder.build();
        restTemplate.delete(
                GET_BEER_BY_ID_PATH,
                id
        );
    }
}
