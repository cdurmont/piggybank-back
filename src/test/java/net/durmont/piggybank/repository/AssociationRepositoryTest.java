package net.durmont.piggybank.repository;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.quarkus.test.vertx.UniAsserter;
import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class AssociationRepositoryTest {

    @InjectMock
    AssociationRepository associationRepository;

    @RunOnVertxContext
    @Test
    void genericTest(UniAsserter asserter) {
        asserter.execute(() -> Mockito.when(associationRepository.count()).thenReturn(Uni.createFrom().item(10L)));
        asserter.assertEquals(() -> associationRepository.count(), 10L);

//        asserter.execute(() -> Mockito.when(associationRepository.findAll()).thenReturn(Uni.createFrom().item(new ArrayList<>())));
//        asserter.assertThat(() -> associationRepository.findAll().list(), list -> list.isEmpty());

    }


    @Test
    void create() {
    }

    @RunOnVertxContext
    @Test
    void list(UniAsserter asserter) {

    }

    @Test
    void update() {
    }

    @Test
    void delete() {
    }
}