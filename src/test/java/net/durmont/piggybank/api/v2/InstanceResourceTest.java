package net.durmont.piggybank.api.v2;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import net.durmont.piggybank.model.Instance;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;

@QuarkusTest
@TestSecurity(user = "testUser", roles = {"user", "admin"})
class InstanceResourceTest {

    @Test
    void read() {
        Instance i = new Instance();
        i.name = "test-" + UUID.randomUUID();
        given().body(i).when().post("/api-v2/instances");

        given()
                .when().get("/api-v2/instances")
                .then()
                    .statusCode(200)
                    .body("$", Matchers.hasItem(Matchers.hasEntry("name", i.name)))
        ;
    }

    @Test
    void readOne() {
        Instance i = new Instance();
        i.name = "test-" + UUID.randomUUID();
        Instance instanceDb = given().body(i).when().post("/api-v2/instances").body().as(Instance.class);


        given()
                .when().get("/api-v2/instances/" + instanceDb.id)
                .then()
                .statusCode(200)
                .body("name", equalTo(i.name))
        ;
    }

    @Test
    void create() {
        Instance i = new Instance();
        i.name = "test-" + UUID.randomUUID();
        given()
                .body(i)
                .when().post("/api-v2/instances")
                .then()
                    .statusCode(201)
                .body("name", equalTo(i.name))
        ;
    }

    @Test
    void update() {
        Instance i = new Instance();
        i.name = "test-" + UUID.randomUUID();
        Instance instanceDb = given().body(i).when().post("/api-v2/instances").body().as(Instance.class);

        Instance modified = new Instance();
        modified.name = "test-" + UUID.randomUUID();
        modified.id = instanceDb.id;
        given().body(modified).when().patch("/api-v2/instances/" + instanceDb.id);


        given()
                .when().get("/api-v2/instances/" + instanceDb.id)
                .then()
                .statusCode(200)
                .body("name", equalTo(modified.name))
        ;

    }

    @Test
    void delete() {
        Instance i = new Instance();
        i.name = "test-" + UUID.randomUUID();
        Instance instanceDb = given().body(i).when().post("/api-v2/instances").body().as(Instance.class);

        given().when().delete("/api-v2/instances/" + instanceDb.id).then().statusCode(204);

        given()
                .when().get("/api-v2/instances/" + instanceDb.id)
                .then()
                .statusCode(404)
        ;
    }
}