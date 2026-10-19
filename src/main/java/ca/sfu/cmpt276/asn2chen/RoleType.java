package ca.sfu.cmpt276.asn2chen;

public enum RoleType {
    TA(new TeachingAssistantProfile()),
    PROF(new ProfessorProfile());
    private final StaffMemberProfile profile;
    RoleType(StaffMemberProfile profile) {
        this.profile = profile;
    }
    public String displayTitle() {
        return profile.displayTitle();
    }
}

interface StaffMemberProfile {
    String displayTitle();
}

final class TeachingAssistantProfile implements StaffMemberProfile {
    @Override
    public String displayTitle() {
        return "Teaching Assistant";
    }
}

final class ProfessorProfile implements StaffMemberProfile {
    @Override
    public String displayTitle() {
        return "Professor";
    }
}
