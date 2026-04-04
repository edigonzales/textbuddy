package app.textbuddy.integration.llm;

import app.textbuddy.advisor.AdvisorRuleCheck;
import app.textbuddy.advisor.AdvisorRuleMatch;

import java.util.List;

public interface AdvisorValidationLlmClient {

    List<AdvisorRuleMatch> validate(String text, List<AdvisorRuleCheck> ruleChecks);
}
