package com.soundcloud.android.configuration;

public class UserPlanChangedEvent {

    private static final int DOWNGRADE = 0;
    private static final int UPGRADE = 1;

    private final int kind;
    final Plan oldPlan;
    final Plan newPlan;

    static UserPlanChangedEvent forDowngrade(Plan oldPlan, Plan newPlan) {
        return new UserPlanChangedEvent(DOWNGRADE, oldPlan, newPlan);
    }

    static UserPlanChangedEvent forUpgrade(Plan oldPlan, Plan newPlan) {
        return new UserPlanChangedEvent(UPGRADE, oldPlan, newPlan);
    }

    private UserPlanChangedEvent(int kind, Plan oldPlan, Plan newPlan) {
        this.kind = kind;
        this.oldPlan = oldPlan;
        this.newPlan = newPlan;
    }

    public boolean isDowngrade() {
        return kind == DOWNGRADE;
    }

    public boolean isUpgrade() {
        return kind == UPGRADE;
    }
}
