"use strict";
var dialogTrigger = null;
var submitting = false;
var leaving = false;
var nextPage = "";
var forms = document.querySelectorAll("[data-submit-form]");
var ratingForm = document.querySelector("[data-rating-form]");
var formFields = [];
var initialValues = [];
var serverErrors = document.querySelector("[data-error-summary]");
var scoreInputs = document.querySelectorAll("[data-score-input]");
var scorePreview = document.querySelector("[data-overall-preview]");
var comment = document.getElementById("comment");
var commentCount = document.querySelector("[data-comment-count]");

function openDialog(dialog, trigger) {
    dialogTrigger = trigger;
    dialog.showModal();
    var cancelButton = dialog.querySelector("[autofocus]");
    if (cancelButton !== null) {
        cancelButton.focus();
    }
}

function closeDialog(event) {
    var dialog = event.currentTarget.closest("dialog");
    dialog.close();
}

function restoreDialogFocus() {
    if (dialogTrigger !== null) {
        dialogTrigger.focus();
    }
}

function showDeleteDialog(event) {
    openDialog(document.getElementById("delete-dialog"), event.currentTarget);
}

var dialogs = document.querySelectorAll("dialog");
for (var i = 0; i < dialogs.length; i++) {
    dialogs[i].addEventListener("close", restoreDialogFocus);
}

var closeButtons = document.querySelectorAll("[data-close-dialog]");
for (var i = 0; i < closeButtons.length; i++) {
    closeButtons[i].addEventListener("click", closeDialog);
    closeButtons[i].disabled = false;
}

var deleteButton = document.querySelector("[data-delete-trigger]");
if (deleteButton !== null) {
    deleteButton.addEventListener("click", showDeleteDialog);
    deleteButton.disabled = false;
    deleteButton.hidden = false;
}

function submitForm(event) {
    var form = event.currentTarget;
    if (form.dataset.submitting === "true") {
        event.preventDefault();
        return;
    }

    submitting = true;
    form.dataset.submitting = "true";
    form.setAttribute("aria-busy", "true");

    var button = form.querySelector('button[type="submit"]');
    button.disabled = true;
    button.textContent = button.dataset.busyLabel;
    button.setAttribute("aria-busy", "true");
}

function checkEnterKey(event) {
    if (event.key === "Enter" && event.isComposing) {
        event.preventDefault();
    }
}

for (var i = 0; i < forms.length; i++) {
    var button = forms[i].querySelector('button[type="submit"]');
    button.dataset.originalLabel = button.textContent;
    forms[i].addEventListener("submit", submitForm);
    forms[i].addEventListener("keydown", checkEnterKey);
}

function resetForms() {
    submitting = false;
    leaving = false;
    for (var i = 0; i < forms.length; i++) {
        forms[i].dataset.submitting = "false";
        forms[i].removeAttribute("aria-busy");
        var button = forms[i].querySelector('button[type="submit"]');
        button.disabled = false;
        button.textContent = button.dataset.originalLabel;
        button.removeAttribute("aria-busy");
    }
    if (ratingForm !== null) {
        updatePreview();
        updateCommentCount();
    }
}
window.addEventListener("pageshow", resetForms);
function readScore(input) {
    var text = input.value.trim();
    for (var score = 1; score <= 10; score++) {
        if (text === String(score)) {
            return score;
        }
    }
    return null;
}

function updatePreview() {
    var total = 0;
    for (var i = 0; i < scoreInputs.length; i++) {
        var score = readScore(scoreInputs[i]);
        if (score === null) {
            scorePreview.textContent = "—";
            return;
        }
        total = total + score;
    }
    scorePreview.textContent = (total / 3).toFixed(1);
}

function updateCommentCount() {
    commentCount.textContent = comment.value.length + " / 500";
}

function hasUnsavedChanges() {
    if (serverErrors !== null) {
        return true;
    }
    for (var i = 0; i < formFields.length; i++) {
        if (formFields[i].value !== initialValues[i]) {
            return true;
        }
    }
    return false;
}

function checkNavigation(event) {
    var link = event.currentTarget;
    if (event.defaultPrevented || event.button !== 0) {
        return;
    }
    if (event.ctrlKey || event.metaKey || event.shiftKey || event.altKey) {
        return;
    }
    if (link.target === "_blank" || link.hash !== "") {
        return;
    }
    if (submitting || !hasUnsavedChanges()) {
        return;
    }

    event.preventDefault();
    nextPage = link.href;
    openDialog(document.getElementById("discard-dialog"), link);
}

function discardChanges() {
    leaving = true;
    window.location.href = nextPage;
}

function checkBeforeUnload(event) {
    if (!submitting && !leaving && hasUnsavedChanges()) {
        event.preventDefault();
        event.returnValue = "";
    }
}

if (ratingForm !== null) {
    formFields = ratingForm.querySelectorAll("input, select, textarea");
    for (var i = 0; i < formFields.length; i++) {
        initialValues.push(formFields[i].value);
    }

    var firstInvalidField = ratingForm.querySelector('[aria-invalid="true"]');
    if (firstInvalidField !== null) {
        firstInvalidField.focus();
    }

    for (var i = 0; i < scoreInputs.length; i++) {
        scoreInputs[i].addEventListener("input", updatePreview);
    }
    comment.addEventListener("input", updateCommentCount);
    updatePreview();
    updateCommentCount();

    var links = document.querySelectorAll("a[href]");
    for (var i = 0; i < links.length; i++) {
        links[i].addEventListener("click", checkNavigation);
    }

    var discardButton = document.querySelector("[data-discard]");
    discardButton.addEventListener("click", discardChanges);
    discardButton.disabled = false;
    window.addEventListener("beforeunload", checkBeforeUnload);
}
