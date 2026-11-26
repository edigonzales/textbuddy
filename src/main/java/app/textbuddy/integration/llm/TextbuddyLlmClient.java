package app.textbuddy.integration.llm;

import app.textbuddy.advisor.AdvisorRuleCheck;
import app.textbuddy.advisor.AdvisorRuleMatch;
import app.textbuddy.quickaction.SummarizePrompt;

import java.util.List;

public interface TextbuddyLlmClient {

    String rewrite(String text, String language, SummarizePrompt summary);

    List<AdvisorRuleMatch> validate(String text, List<AdvisorRuleCheck> ruleChecks);
}
