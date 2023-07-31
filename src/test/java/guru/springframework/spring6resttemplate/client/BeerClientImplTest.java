package guru.springframework.spring6resttemplate.client;

import guru.springframework.spring6resttemplate.model.BeerDTO;
import guru.springframework.spring6resttemplate.model.BeerStyle;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.client.HttpClientErrorException;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class BeerClientImplTest {
    @Autowired
    BeerClient beerClient;

    @Test
    void listBeers() {
        beerClient.listBeers(null, BeerStyle.ALE, null, null, null);
    }

    @Test
    void listBeersNoName() {
        beerClient.listBeers();
    }

    @Test
    void getBeerById() {
        BeerDTO beerDTO = beerClient.listBeers().getContent().get(0);
        BeerDTO beerById = beerClient.getBeerById(beerDTO.getId());
        assertNotNull(beerById);
    }

    @Test
    void createBeer() {
        BeerDTO newDto = BeerDTO.builder()
                .beerName("New Beer")
                .beerStyle(BeerStyle.IPA)
                .upc("123456")
                .quantityOnHand(500)
                .price(new BigDecimal("7.39"))
                .build();
        BeerDTO savedDto = beerClient.createBeer(newDto);
        assertNotNull(savedDto);
    }

    @Test
    void updateBeer() {
        BeerDTO beerDTO = beerClient.listBeers().getContent().get(0);
        beerDTO.setBeerName("Updated Name");
        BeerDTO updatedBeer = beerClient.updateBeer(beerDTO);
        assertEquals(beerDTO.getBeerName(), updatedBeer.getBeerName());
    }

    @Test
    void deleteBeer() {
        BeerDTO beerDTO = beerClient.listBeers().getContent().get(0);
        beerClient.deleteBeer(beerDTO.getId());
        assertThrows(HttpClientErrorException.class, () -> {
            beerClient.getBeerById(beerDTO.getId());
        });
    }
}
