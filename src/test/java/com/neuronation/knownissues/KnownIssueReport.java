package com.neuronation.knownissues;

import com.google.gson.GsonBuilder;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Machine-readable summary of the known-issue state for one run: every active entry, which ones
 * actually fired (expected-fail), and which are past their review date (aging). Serialized to
 * target/known-issues-report.json for the Jenkins/Slack summary.
 */
public final class KnownIssueReport {

    /** One row of the report. Fields are serialized to JSON by Gson. */
    public static final class Item {
        String id;
        String key;
        String jira;
        String description;
        boolean encountered;
        boolean aging;
    }

    private final List<Item> knownIssues;
    private final int encounteredCount;
    private final List<String> agingKeys;

    private KnownIssueReport(List<Item> items) {
        this.knownIssues = items;
        this.encounteredCount = (int) items.stream().filter(i -> i.encountered).count();
        this.agingKeys = items.stream().filter(i -> i.aging).map(i -> i.key).toList();
    }

    public static KnownIssueReport build(Collection<KnownIssue> activeEntries,
                                         Set<String> encounteredIds, LocalDate today) {
        List<Item> items = new ArrayList<>();
        for (KnownIssue ki : activeEntries) {
            Item it = new Item();
            it.id = ki.getId();
            it.key = ki.jiraKey();
            it.jira = ki.getJira();
            it.description = ki.getDescription();
            it.encountered = encounteredIds.contains(ki.getId());
            it.aging = ki.isAging(today);
            items.add(it);
        }
        return new KnownIssueReport(items);
    }

    public int total() { return knownIssues.size(); }
    public int encounteredCount() { return encounteredCount; }

    public List<String> encounteredKeys() {
        return knownIssues.stream().filter(i -> i.encountered).map(i -> i.key).toList();
    }

    public List<String> agingKeys() { return agingKeys; }

    public String toJson() {
        return new GsonBuilder().setPrettyPrinting().create().toJson(this);
    }
}
