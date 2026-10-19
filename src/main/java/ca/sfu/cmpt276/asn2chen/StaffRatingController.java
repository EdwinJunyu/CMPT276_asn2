package ca.sfu.cmpt276.asn2chen;

import java.beans.PropertyEditorSupport;
import java.sql.SQLException;
import java.util.Locale;

import jakarta.validation.Valid;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class StaffRatingController {

    private final StaffRatingRepository repository;

    public StaffRatingController(StaffRatingRepository repository) {
        this.repository = repository;
    }

    @InitBinder("staffRating")
    public void configureFormBinding(WebDataBinder binder) {
        binder.setAllowedFields("name", "email", "roleType", "clarity", "niceness",
                "knowledgeableScore", "comment");
        binder.registerCustomEditor(String.class, new StringTrimmerEditor(true));
        binder.registerCustomEditor(String.class, "email", new PropertyEditorSupport() {
            @Override
            public void setAsText(String text) {
                setValue(text == null ? null : text.trim().toLowerCase(Locale.ROOT));
            }
        });
    }

    @GetMapping("/")
    public String home() {
        return "redirect:/ratings";
    }

    @GetMapping("/ratings")
    public String list(Model model) {
        model.addAttribute("ratings", repository.findAllByOrderByIdDesc());
        // 视图名相对于 templates 目录，对应 ratings/index.html。
        return "ratings/index";
    }

    @GetMapping("/ratings/{id}")
    public String detail(@PathVariable("id") Long id, Model model) {
        model.addAttribute("rating", findRating(id));
        return "ratings/detail";
    }

    @GetMapping("/ratings/new")
    public String newForm(Model model) {
        model.addAttribute("staffRating", new StaffRating());
        return formView(model, null);
    }

    @PostMapping("/ratings")
    public String create(@Valid @ModelAttribute("staffRating") StaffRating form,
            BindingResult errors, Model model, RedirectAttributes redirectAttributes) {
        return save(form, errors, model, null, redirectAttributes);
    }

    @GetMapping("/ratings/{id}/edit")
    public String editForm(@PathVariable("id") Long id, Model model) {
        model.addAttribute("staffRating", findRating(id));
        return formView(model, id);
    }

    @PostMapping("/ratings/{id}/edit")
    public String update(@PathVariable("id") Long id,
            @Valid @ModelAttribute("staffRating") StaffRating form,
            BindingResult errors, Model model, RedirectAttributes redirectAttributes) {
        return save(form, errors, model, id, redirectAttributes);
    }

    @PostMapping("/ratings/{id}/delete")
    public String delete(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        repository.delete(findRating(id));
        redirectAttributes.addFlashAttribute("successMessage", "Staff rating deleted.");
        return "redirect:/ratings";
    }

    private String save(StaffRating form, BindingResult errors, Model model, Long id,
            RedirectAttributes redirectAttributes) {
        StaffRating target = id == null ? form : findRating(id);
        if (!errors.hasErrors() && emailExists(form.getEmail(), id)) {
            rejectDuplicateEmail(errors);
        }
        if (errors.hasErrors()) {
            return formView(model, id);
        }

        if (id != null) {
            target.setName(form.getName());
            target.setEmail(form.getEmail());
            target.setRoleType(form.getRoleType());
            target.setClarity(form.getClarity());
            target.setNiceness(form.getNiceness());
            target.setKnowledgeableScore(form.getKnowledgeableScore());
            target.setComment(form.getComment());
        }

        try {
            repository.saveAndFlush(target);
        } catch (DataIntegrityViolationException exception) {
            if (!isEmailConflict(exception, form.getEmail(), id)) {
                throw exception;
            }
            rejectDuplicateEmail(errors);
            return formView(model, id);
        }
        redirectAttributes.addFlashAttribute("successMessage",
                id == null ? "Staff rating added." : "Changes saved.");
        return "redirect:/ratings/" + target.getId();
    }

    private StaffRating findRating(Long id) {
        return repository.findById(id).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Rating not found."));
    }

    private String formView(Model model, Long id) {
        model.addAttribute("roleTypes", RoleType.values());
        model.addAttribute("editing", id != null);
        model.addAttribute("ratingId", id);
        return "ratings/form";
    }

    private boolean emailExists(String email, Long id) {
        return id == null ? repository.existsByEmail(email)
                : repository.existsByEmailAndIdNot(email, id);
    }

    private void rejectDuplicateEmail(BindingResult errors) {
        errors.rejectValue("email", "duplicate", "This email already has a rating.");
    }

    private boolean isEmailConflict(DataIntegrityViolationException exception,
            String email, Long id) {
        boolean uniqueViolation = false;
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            if (cause instanceof ConstraintViolationException constraint) {
                String name = constraint.getConstraintName();
                if (name != null && !name.isBlank()) {
                    return name.toLowerCase(Locale.ROOT).contains("uk_staff_rating_email");
                }
            }
            if (cause instanceof SQLException sql && "23505".equals(sql.getSQLState())) {
                uniqueViolation = true;
            }
        }
        return uniqueViolation && emailExists(email, id);
    }
}
