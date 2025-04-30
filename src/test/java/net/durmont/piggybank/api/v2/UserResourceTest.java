package net.durmont.piggybank.api.v2;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import net.durmont.piggybank.model.User;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;

@QuarkusTest
@TestSecurity(user = "testUser", roles = {"user", "admin"})
class UserResourceTest {

    @Test
    void read() {
        // given
        String testSessionId = UUID.randomUUID().toString();
        System.out.println("testSessionId: " + testSessionId);
        User user1 = new User();
        user1.admin = false;
        user1.login = "test-" + testSessionId + "-1";
        user1.name = "Test User";
        User user2 = new User();
        user2.admin = false;
        user2.login = "test2-" + testSessionId + "-2";
        user2.name = "Test User 2";

        given().body(user1).when().post("/api-v2/users").body().as(User.class);
        given().body(user2).when().post("/api-v2/users").body().as(User.class);

        // when/then
        // no filter
        List<User> users = Arrays
                .stream(given().when().get("/api-v2/users").body().as(User[].class))
                .filter(u -> u.login.contains(testSessionId)).toList();
        System.out.println(users.stream().map(u -> u.login).toList());
        assert users.size() == 2;
        assert users.stream().anyMatch(u -> u.login.equals(user1.login));
        assert users.stream().anyMatch(u -> u.login.equals(user2.login));

        // filter on name
        users = Arrays
                .stream(given().queryParam("filter",user1).when().get("/api-v2/users").body().as(User[].class))
                .filter(u -> u.login.startsWith("test-"+ testSessionId)).toList();
        assert users.size() == 1;
        assert users.stream().anyMatch(u -> u.login.equals(user1.login));
    }

    @Test
    void readOne() {
        User user = new User();
        user.admin = false;
        user.login = "test-" + UUID.randomUUID();
        user.name = "Test User";

        User userDb = given().body(user).when().post("/api-v2/users").body().as(User.class);

        given()
                .when().get("/api-v2/users/" + userDb.id)
                .then()
                    .statusCode(200)
                    .body("name", equalTo(user.name))
                    .body("login", equalTo(user.login))
                    .body("admin", equalTo(user.admin))
        ;

        given()
                .when().get("/api-v2/users/" + UUID.randomUUID())
                .then()
                .statusCode(404);
    }

    @Test
    void create() {
        User user = new User();
        user.admin = false;
        user.login = "test-" + UUID.randomUUID();
        user.name = "Test User";

        given()
                .body(user)
                .when().post("/api-v2/users")
                .then()
                    .statusCode(201)
                    .body("name", equalTo(user.name))
                    .body("login", equalTo(user.login))
                    .body("admin", equalTo(user.admin))
        ;
    }

    @Test
    void update() {
        User user = new User();
        user.admin = false;
        user.login = "test-" + UUID.randomUUID();
        user.name = "Test User";
        User userDb = given().body(user).when().post("/api-v2/users").body().as(User.class);

        userDb.name = "Updated name";
        userDb.admin = true;
        given().body(userDb).when().put("/api-v2/users/" + userDb.id);

        given()
                .when().get("/api-v2/users/" + userDb.id)
                .then()
                    .statusCode(200)
                    .body("name", equalTo(userDb.name))
                    .body("login", equalTo(userDb.login))
                    .body("admin", equalTo(userDb.admin));
    }

    @Test
    void delete() {
        User user = new User();
        user.admin = false;
        user.login = "test-" + UUID.randomUUID();
        user.name = "Test User";
        User userDb = given().body(user).when().post("/api-v2/users").body().as(User.class);

        given().when().delete("/api-v2/users/" + userDb.id).then().statusCode(204);

        given().when().get("/api-v2/users/" + userDb.id).then().statusCode(404);
    }
}