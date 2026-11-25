package app.textbuddy.integration.llm;

import app.textbuddy.advisor.AdvisorFixFinding;
import app.textbuddy.advisor.AdvisorRuleCheck;
import app.textbuddy.advisor.AdvisorRuleMatch;
import app.textbuddy.quickaction.QuickActionRequest;
import app.textbuddy.quickaction.QuickActionType;

import java.util.List;

public interface TextbuddyLlmClient {

    String rewrite(QuickActionType action, QuickActionRequest request);

    List<AdvisorRuleMatch> validate(String text, List<AdvisorRuleCheck> ruleChecks);

    String fixAdvisor(String text, List<AdvisorFixFinding> findings);
}
