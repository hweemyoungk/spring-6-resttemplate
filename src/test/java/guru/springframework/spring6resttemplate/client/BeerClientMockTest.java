package guru.springframework.spring6resttemplate.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import guru.springframework.spring6resttemplate.config.OAuthClientInterceptor;
import guru.springframework.spring6resttemplate.config.RestTemplateBuilderConfig;
import guru.springframework.spring6resttemplate.model.BeerDTO;
import guru.springframework.spring6resttemplate.model.BeerDtoPageImpl;
import guru.springframework.spring6resttemplate.model.BeerStyle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.MockServerRestTemplateCustomizer;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.InMemoryOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import java.util.Arrays;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

/**
 * Created by jt, Spring Framework Guru.
 */
@RestClientTest
@Import({RestTemplateBuilderConfig.class, OAuthClientInterceptor.class})
public class BeerClientMockTest {

    static final String URL = "http://localhost:8080";

    //@Autowired
    BeerClient beerClient;

    MockRestServiceServer server;

    @Autowired
    RestTemplateBuilder restTemplateBuilderConfigured; // Bean created by RestTemplateBuilderConfig

    @Autowired
    ObjectMapper objectMapper;

    @Mock
    RestTemplateBuilder mockRestTemplateBuilder = new RestTemplateBuilder(new MockServerRestTemplateCustomizer());
    private BeerDTO dto;
    private String dtoPayload;

    @MockBean
    OAuth2AuthorizedClientManager manager;

    @TestConfiguration
    public static class TestConfig {
        @Bean
        ClientRegistrationRepository clientRegistrationRepository() {
            return new InMemoryClientRegistrationRepository(ClientRegistration
                    .withRegistrationId("springauth")
                    .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                    .clientId("test-client-id")
                    .tokenUri("test-token-uri")
                    .build()
            );
        }

        @Bean
        OAuth2AuthorizedClientService oAuth2AuthorizedClientService(ClientRegistrationRepository clientRegistrationRepository) {
            return new InMemoryOAuth2AuthorizedClientService(clientRegistrationRepository);
        }
    }

    @Autowired
    ClientRegistrationRepository clientRegistrationRepository;

    @BeforeEach
    void setUp() throws JsonProcessingException {
        ClientRegistration clientRegistration = clientRegistrationRepository
                .findByRegistrationId("springauth");
        OAuth2AccessToken token = new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER,
                "test-token-uri",
                Instant.MIN,
                Instant.MAX
        );
        when(manager.authorize(any())).thenReturn(new OAuth2AuthorizedClient(
                clientRegistration,
                "test-principal-name",
                token
        ));

        RestTemplate restTemplate = restTemplateBuilderConfigured.build();
        server = MockRestServiceServer.bindTo(restTemplate).build();
        when(mockRestTemplateBuilder.build()).thenReturn(restTemplate);
        beerClient = new BeerClientImpl(mockRestTemplateBuilder);

        dto = getBeerDto();
        dtoPayload = objectMapper.writeValueAsString(dto);
    }

    @Test
    void testGetById() throws JsonProcessingException {
        serverMockGet();
        BeerDTO beerById = beerClient.getBeerById(dto.getId());
        assertEquals(beerById.getId(), dto.getId());
    }

    @Test
    void testListBeers() throws JsonProcessingException {
        String payload = objectMapper.writeValueAsString(getPage());

        server.expect(method(HttpMethod.GET))
                //.andExpect(header("Authorization", "Basic dXNlcjE6c2VjcmV0"))
                .andExpect(header("Authorization", "Bearer test-token-uri"))
                .andExpect(requestTo(URL + BeerClientImpl.GET_BEER_PATH))
                .andRespond(withSuccess(payload, MediaType.APPLICATION_JSON));

        Page<BeerDTO> dtos = beerClient.listBeers();
        assertThat(dtos.getContent().size()).isGreaterThan(0);
    }

    @Test
    void testListBeersWithQueryParams() throws JsonProcessingException {
        String payload = objectMapper.writeValueAsString(getPage());
        URI uri = UriComponentsBuilder
                .fromPath(BeerClientImpl.GET_BEER_PATH)
                .queryParam("beerName", "ALE")
                .build().toUri();
        server.expect(method(HttpMethod.GET))
                //.andExpect(header("Authorization", "Basic dXNlcjE6c2VjcmV0"))
                .andExpect(header("Authorization", "Bearer test-token-uri"))
                .andExpect(requestTo(URL + uri))
                .andRespond(withSuccess(payload, MediaType.APPLICATION_JSON));
        Page<BeerDTO> beerDTOS = beerClient.listBeers("ALE", null, null, null, null);
        assertEquals(beerDTOS.getSize(), getPage().getSize());
        server.verify();
    }

    @Test
    void testCreateBeer() throws JsonProcessingException {
        URI location = UriComponentsBuilder
                .fromPath(BeerClientImpl.GET_BEER_BY_ID_PATH)
                .build(dto.getId());
        server.expect(method(HttpMethod.POST))
                //.andExpect(header("Authorization", "Basic dXNlcjE6c2VjcmV0"))
                .andExpect(header("Authorization", "Bearer test-token-uri"))
                .andExpect(requestTo(
                        URL + BeerClientImpl.GET_BEER_PATH
                ))
                .andRespond(withAccepted().location(location));
        serverMockGet();

        BeerDTO newBeer = beerClient.createBeer(dto);

        assertEquals(newBeer.getId(), dto.getId());
    }

    @Test
    void testUpdateBeer() {
        server.expect(method(HttpMethod.PUT))
                //.andExpect(header("Authorization", "Basic dXNlcjE6c2VjcmV0"))
                .andExpect(header("Authorization", "Bearer test-token-uri"))
                .andExpect(requestToUriTemplate(
                        URL + BeerClientImpl.GET_BEER_BY_ID_PATH,
                        dto.getId()))
                .andRespond(withNoContent());
        serverMockGet();

        BeerDTO updatedBeer = beerClient.updateBeer(dto);
        assertEquals(updatedBeer.getId(), dto.getId());
    }

    @Test
    void testDeleteBeer() {
        server.expect(method(HttpMethod.DELETE))
                //.andExpect(header("Authorization", "Basic dXNlcjE6c2VjcmV0"))
                .andExpect(header("Authorization", "Bearer test-token-uri"))
                .andExpect(requestToUriTemplate(
                        URL + BeerClientImpl.GET_BEER_BY_ID_PATH,
                        dto.getId()))
                .andRespond(withNoContent());
        beerClient.deleteBeer(dto.getId());
        server.verify();
    }

    @Test
    void testDeleteBeerNotFound() {
        server.expect(method(HttpMethod.DELETE))
                //.andExpect(header("Authorization", "Basic dXNlcjE6c2VjcmV0"))
                .andExpect(header("Authorization", "Bearer test-token-uri"))
                .andExpect(requestToUriTemplate(
                        URL + BeerClientImpl.GET_BEER_BY_ID_PATH,
                        dto.getId()))
                .andRespond(withResourceNotFound());
        assertThrows(RestClientException.class, () -> {
            beerClient.deleteBeer(dto.getId());
        });
        server.verify();

    }

    private void serverMockGet() {
        server.expect(method(HttpMethod.GET))
                //.andExpect(header("Authorization", "Basic dXNlcjE6c2VjcmV0"))
                .andExpect(header("Authorization", "Bearer test-token-uri"))
                .andExpect(requestToUriTemplate(URL + BeerClientImpl.GET_BEER_BY_ID_PATH, dto.getId()))
                .andRespond(withSuccess(dtoPayload, MediaType.APPLICATION_JSON));
    }

    BeerDTO getBeerDto() {
        return BeerDTO.builder()
                .id(UUID.randomUUID())
                .price(new BigDecimal("10.99"))
                .beerName("Mango Bobs")
                .beerStyle(BeerStyle.IPA)
                .quantityOnHand(500)
                .upc("123245")
                .build();
    }

    BeerDtoPageImpl getPage() {
        return new BeerDtoPageImpl<>(Arrays.asList(getBeerDto()), 1, 25, 1);
    }
}
