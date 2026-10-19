package ca.sfu.cmpt276.asn2chen;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.time.Instant;
import java.util.List;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:staff-rating-tests;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
@Transactional
class Asn2ChenApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StaffRatingRepository repository;

    @Autowired
    private EntityManager entityManager;

    @ParameterizedTest
    @ValueSource(strings = {"not-an-email", "staff@", "@example.com"})
    void createRejectsInvalidEmail(String email) throws Exception {
        MultiValueMap<String, String> form = validForm();
        form.set("email", email);

        assertCreateRejected(form, "email");
    }

    @ParameterizedTest
    @CsvSource({
            "clarity, 0", "clarity, 11",
            "niceness, 0", "niceness, 11",
            "knowledgeableScore, 0", "knowledgeableScore, 11"
    })
    void createRejectsOutOfRangeScore(String field, String score) throws Exception {
        MultiValueMap<String, String> form = validForm();
        form.set(field, score);

        assertCreateRejected(form, field);
    }

    @ParameterizedTest
    @ValueSource(strings = {"name", "email", "roleType", "clarity", "niceness", "knowledgeableScore"})
    void createRejectsMissingRequiredField(String field) throws Exception {
        MultiValueMap<String, String> form = validForm();
        form.remove(field);

        assertCreateRejected(form, field);
    }

    @ParameterizedTest
    @ValueSource(strings = {"name", "email"})
    void createRejectsBlankRequiredText(String field) throws Exception {
        MultiValueMap<String, String> form = validForm();
        form.set(field, "   ");

        assertCreateRejected(form, field);
    }

    @ParameterizedTest
    @CsvSource({
            "clarity, abc", "clarity, 1.5",
            "niceness, abc", "niceness, 1.5",
            "knowledgeableScore, abc", "knowledgeableScore, 1.5",
            "roleType, LECTURER"
    })
    void createRejectsInvalidFieldType(String field, String value) throws Exception {
        MultiValueMap<String, String> form = validForm();
        form.set(field, value);

        assertCreateRejected(form, field);
    }

    @Test
    void createRejectsEmptyFormWithAllRequiredFieldErrors() throws Exception {
        mockMvc.perform(post("/ratings"))
                .andExpect(status().isOk())
                .andExpect(view().name("ratings/form"))
                .andExpect(model().attributeHasFieldErrors("staffRating",
                        "name", "email", "roleType", "clarity", "niceness", "knowledgeableScore"));

        assertThat(repository.count()).isZero();
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 10})
    void createAcceptsScoreBoundariesAndOptionalComment(int score) throws Exception {
        MultiValueMap<String, String> form = validForm();
        form.set("clarity", Integer.toString(score));
        form.set("niceness", Integer.toString(score));
        form.set("knowledgeableScore", Integer.toString(score));
        form.remove("comment");

        mockMvc.perform(post("/ratings").params(form))
                .andExpect(status().is3xxRedirection());

        entityManager.clear();
        assertThat(repository.findAll()).singleElement().satisfies(saved -> {
            assertThat(saved.getClarity()).isEqualTo(score);
            assertThat(saved.getNiceness()).isEqualTo(score);
            assertThat(saved.getKnowledgeableScore()).isEqualTo(score);
            assertThat(saved.getComment()).isNull();
        });
    }

    @Test
    void homeRedirectsToRatingsIndex() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/ratings"));
    }

    @Test
    void indexReturns200AndEmptyRatingsModel() throws Exception {
        mockMvc.perform(get("/ratings"))
                .andExpect(status().isOk())
                .andExpect(view().name("ratings/index"))
                .andExpect(model().attribute("ratings", empty()))
                .andExpect(content().string(containsString("No staff ratings yet.")));
    }

    @Test
    void indexIncludesSavedRatingsWithNewestFirst() throws Exception {
        StaffRating older = repository.saveAndFlush(newRating("older@example.com"));
        StaffRating newer = repository.saveAndFlush(newRating("newer@example.com"));

        MvcResult result = mockMvc.perform(get("/ratings"))
                .andExpect(status().isOk())
                .andExpect(view().name("ratings/index"))
                .andExpect(model().attribute("ratings", hasSize(2)))
                .andExpect(content().string(containsString("older@example.com")))
                .andExpect(content().string(containsString("newer@example.com")))
                .andReturn();

        List<?> ratings = (List<?>) result.getModelAndView().getModel().get("ratings");
        assertThat(ratings).extracting("id").containsExactly(newer.getId(), older.getId());
    }

    @Test
    void newFormIncludesExpectedModelAttributes() throws Exception {
        mockMvc.perform(get("/ratings/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("ratings/form"))
                .andExpect(model().attributeExists("staffRating"))
                .andExpect(model().attribute("roleTypes", RoleType.values()))
                .andExpect(model().attribute("editing", false))
                .andExpect(content().string(containsString("Add staff rating")));
    }

    @Test
    void createSavesNormalizedInputAndRedirectsToDetail() throws Exception {
        MultiValueMap<String, String> form = validForm();
        form.set("name", "  Ada Lovelace  ");
        form.set("email", "  ADA@EXAMPLE.COM  ");

        MvcResult result = mockMvc.perform(post("/ratings").params(form))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("successMessage", "Staff rating added."))
                .andReturn();

        entityManager.clear();
        assertThat(repository.count()).isEqualTo(1);
        StaffRating saved = repository.findAll().getFirst();
        assertThat(saved.getName()).isEqualTo("Ada Lovelace");
        assertThat(saved.getEmail()).isEqualTo("ada@example.com");
        assertThat(saved.getRoleType()).isEqualTo(RoleType.PROF);
        assertThat(saved.getClarity()).isEqualTo(7);
        assertThat(saved.getNiceness()).isEqualTo(8);
        assertThat(saved.getKnowledgeableScore()).isEqualTo(10);
        assertThat(saved.getComment()).isEqualTo("讲解清楚，反馈及时。");
        assertThat(result.getResponse().getRedirectedUrl()).isEqualTo("/ratings/" + saved.getId());

        mockMvc.perform(get(result.getResponse().getRedirectedUrl()))
                .andExpect(status().isOk())
                .andExpect(view().name("ratings/detail"))
                .andExpect(model().attributeExists("rating"))
                .andExpect(content().string(containsString("Ada Lovelace")))
                .andExpect(content().string(containsString("讲解清楚，反馈及时。")))
                .andExpect(content().string(containsString("8.3")));
    }

    @Test
    void createRejectsDuplicateEmailAfterNormalization() throws Exception {
        StaffRating existing = repository.saveAndFlush(newRating("ada@example.com"));
        MultiValueMap<String, String> form = validForm();
        form.set("email", "  ADA@EXAMPLE.COM  ");

        assertCreateRejected(form, "email");

        entityManager.clear();
        assertThat(repository.findAll()).extracting(StaffRating::getId).containsExactly(existing.getId());
    }

    @Test
    void editFormIncludesExistingRatingAndFormOptions() throws Exception {
        StaffRating saved = repository.saveAndFlush(newRating("ada@example.com"));

        mockMvc.perform(get("/ratings/{id}/edit", saved.getId()))
                .andExpect(status().isOk())
                .andExpect(view().name("ratings/form"))
                .andExpect(model().attribute("staffRating", saved))
                .andExpect(model().attribute("roleTypes", RoleType.values()))
                .andExpect(model().attribute("editing", true))
                .andExpect(model().attribute("ratingId", saved.getId()));
    }

    @Test
    void editUpdatesExistingRatingAndRedirectsToDetail() throws Exception {
        StaffRating saved = repository.saveAndFlush(newRating("ada@example.com"));
        Long id = saved.getId();
        entityManager.refresh(saved);
        Instant createdAt = saved.getCreatedAt();
        MultiValueMap<String, String> form = validForm();
        form.set("name", "Updated Staff");
        form.set("email", "updated@example.com");
        form.set("roleType", "TA");
        form.set("clarity", "9");
        form.set("niceness", "6");
        form.set("knowledgeableScore", "8");
        form.set("comment", "修改后的评价");

        mockMvc.perform(post("/ratings/{id}/edit", id).params(form))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/ratings/" + id))
                .andExpect(flash().attribute("successMessage", "Changes saved."));

        entityManager.clear();
        StaffRating updated = repository.findById(id).orElseThrow();
        assertThat(repository.count()).isEqualTo(1);
        assertThat(updated.getName()).isEqualTo("Updated Staff");
        assertThat(updated.getEmail()).isEqualTo("updated@example.com");
        assertThat(updated.getRoleType()).isEqualTo(RoleType.TA);
        assertThat(updated.getClarity()).isEqualTo(9);
        assertThat(updated.getNiceness()).isEqualTo(6);
        assertThat(updated.getKnowledgeableScore()).isEqualTo(8);
        assertThat(updated.getComment()).isEqualTo("修改后的评价");
        assertThat(updated.getCreatedAt()).isEqualTo(createdAt);
    }

    @Test
    void invalidEditReturnsErrorsAndKeepsStoredRatingUnchanged() throws Exception {
        StaffRating saved = repository.saveAndFlush(newRating("ada@example.com"));
        MultiValueMap<String, String> form = validForm();
        form.set("name", "Unsaved Name");
        form.set("clarity", "11");

        mockMvc.perform(post("/ratings/{id}/edit", saved.getId()).params(form))
                .andExpect(status().isOk())
                .andExpect(view().name("ratings/form"))
                .andExpect(model().attributeHasFieldErrors("staffRating", "clarity"))
                .andExpect(model().attribute("editing", true))
                .andExpect(model().attribute("ratingId", saved.getId()))
                .andExpect(content().string(containsString("Unsaved Name")));

        repository.flush();
        entityManager.clear();
        StaffRating unchanged = repository.findById(saved.getId()).orElseThrow();
        assertThat(unchanged.getName()).isEqualTo("Ada Lovelace");
        assertThat(unchanged.getClarity()).isEqualTo(7);
        assertThat(repository.count()).isEqualTo(1);
    }

    @Test
    void editAllowsKeepingOwnEmail() throws Exception {
        StaffRating saved = repository.saveAndFlush(newRating("ada@example.com"));

        mockMvc.perform(post("/ratings/{id}/edit", saved.getId()).params(validForm()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/ratings/" + saved.getId()));

        assertThat(repository.count()).isEqualTo(1);
    }

    @Test
    void deleteRemovesSelectedRatingAndRedirectsToIndex() throws Exception {
        StaffRating removed = repository.saveAndFlush(newRating("remove@example.com"));
        StaffRating retained = repository.saveAndFlush(newRating("retain@example.com"));

        mockMvc.perform(post("/ratings/{id}/delete", removed.getId()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/ratings"))
                .andExpect(flash().attribute("successMessage", "Staff rating deleted."));

        repository.flush();
        entityManager.clear();
        assertThat(repository.findById(removed.getId())).isEmpty();
        assertThat(repository.findById(retained.getId())).isPresent();
        assertThat(repository.count()).isEqualTo(1);
    }

    @ParameterizedTest
    @CsvSource({
            "GET, /ratings/999999999",
            "GET, /ratings/999999999/edit",
            "POST, /ratings/999999999/edit",
            "POST, /ratings/999999999/delete"
    })
    void unknownRatingReturns404(String method, String path) throws Exception {
        mockMvc.perform(request(HttpMethod.valueOf(method), path).params(validForm()))
                .andExpect(status().isNotFound());

        assertThat(repository.count()).isZero();
    }

    @Test
    void repositorySavesAndRetrievesAllRatingFields() {
        StaffRating original = newRating("persist@example.com");
        StaffRating saved = repository.saveAndFlush(original);
        Long id = saved.getId();
        entityManager.clear();

        StaffRating reloaded = repository.findById(id).orElseThrow();
        assertThat(reloaded).isNotSameAs(original);
        assertThat(reloaded.getId()).isPositive();
        assertThat(reloaded.getName()).isEqualTo("Ada Lovelace");
        assertThat(reloaded.getEmail()).isEqualTo("persist@example.com");
        assertThat(reloaded.getRoleType()).isEqualTo(RoleType.PROF);
        assertThat(reloaded.getClarity()).isEqualTo(7);
        assertThat(reloaded.getNiceness()).isEqualTo(8);
        assertThat(reloaded.getKnowledgeableScore()).isEqualTo(10);
        assertThat(reloaded.getComment()).isEqualTo("讲解清楚，反馈及时。");
        assertThat(reloaded.getCreatedAt()).isNotNull();
        assertThat(reloaded.getUpdatedAt()).isEqualTo(reloaded.getCreatedAt());
        assertThat(reloaded.getOverallScore()).isEqualTo(25.0 / 3.0);
    }

    @Test
    void repositoryDeleteRemovesEntryFromDatabase() {
        Long id = repository.saveAndFlush(newRating("delete@example.com")).getId();
        entityManager.clear();
        assertThat(repository.findById(id)).isPresent();

        repository.deleteById(id);
        repository.flush();
        entityManager.clear();

        assertThat(repository.findById(id)).isEmpty();
        assertThat(repository.count()).isZero();
    }

    private MultiValueMap<String, String> validForm() {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("name", "Ada Lovelace");
        form.add("email", "ada@example.com");
        form.add("roleType", "PROF");
        form.add("clarity", "7");
        form.add("niceness", "8");
        form.add("knowledgeableScore", "10");
        form.add("comment", "讲解清楚，反馈及时。");
        return form;
    }

    private void assertCreateRejected(MultiValueMap<String, String> form, String field) throws Exception {
        long countBefore = repository.count();

        mockMvc.perform(post("/ratings").params(form))
                .andExpect(status().isOk())
                .andExpect(view().name("ratings/form"))
                .andExpect(model().attributeHasFieldErrors("staffRating", field))
                .andExpect(model().attribute("roleTypes", RoleType.values()))
                .andExpect(model().attribute("editing", false))
                .andExpect(content().string(containsString("Check the fields below and try again.")))
                .andExpect(content().string(containsString("讲解清楚，反馈及时。")));

        assertThat(repository.count()).isEqualTo(countBefore);
    }

    private StaffRating newRating(String email) {
        StaffRating rating = new StaffRating();
        rating.setName("Ada Lovelace");
        rating.setEmail(email);
        rating.setRoleType(RoleType.PROF);
        rating.setClarity(7);
        rating.setNiceness(8);
        rating.setKnowledgeableScore(10);
        rating.setComment("讲解清楚，反馈及时。");
        return rating;
    }
}
