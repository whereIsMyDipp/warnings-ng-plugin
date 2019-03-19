package io.jenkins.plugins.analysis.core.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.CheckForNull;

import net.sf.json.JSONObject;

import org.kohsuke.stapler.bind.JavaScriptMethod;
import hudson.model.Action;
import hudson.model.Job;
import hudson.model.Run;
import jenkins.model.Jenkins;

import io.jenkins.plugins.analysis.core.charts.ChartModelConfiguration;
import io.jenkins.plugins.analysis.core.charts.CompositeResult;
import io.jenkins.plugins.analysis.core.charts.LinesChartModel;
import io.jenkins.plugins.analysis.core.charts.SeverityTrendChart;
import io.jenkins.plugins.analysis.core.util.StaticAnalysisRun;

/**
 * FIXME: comment class.
 *
 * @author Ullrich Hafner
 */
public class AggregatedTrendAction implements Action {
    private static final int MIN_TOOLS = 2;

    private final Job<?, ?> owner;

    public AggregatedTrendAction(final Job<?, ?> owner) {
        this.owner = owner;
    }

    private Set<AnalysisHistory> createBuildHistory() {
        Run<?, ?> lastFinishedRun = owner.getLastCompletedBuild();
        if (lastFinishedRun == null) {
            return new HashSet<>();
        }
        else {
            return owner.getActions(JobAction.class)
                    .stream()
                    .map(JobAction::getId)
                    .map(id -> new AnalysisHistory(lastFinishedRun, new ByIdResultSelector(id)))
                    .collect(Collectors.toSet());
        }
    }

    /**
     * Returns the UI model for an ECharts line chart that shows the issues stacked by severity.
     *
     * @return the UI model as JSON
     */
    @JavaScriptMethod
    @SuppressWarnings("unused") // Called by jelly view
    public JSONObject getBuildTrend() {
        return JSONObject.fromObject(createChartModel());
    }

    private LinesChartModel createChartModel() {
        List<Iterable<? extends StaticAnalysisRun>> histories = new ArrayList<>(createBuildHistory());

        return new SeverityTrendChart().create(new CompositeResult(histories), new ChartModelConfiguration());
    }

    /**
     * Returns whether the trend chart is visible or not.
     *
     * @return {@code true} if the trend is visible, false otherwise
     */
    @SuppressWarnings("unused") // Called by jelly view
    public boolean isTrendVisible() {
        Set<AnalysisHistory> history = createBuildHistory();

        if (history.size() < MIN_TOOLS) {
            return false;
        }

        AnalysisHistory singleResult = history.iterator().next();
        return singleResult.hasMultipleResults();
    }

    @CheckForNull
    @Override
    public String getIconFileName() {
        return Jenkins.RESOURCE_PATH + "/plugin/warnings-ng/icons/analysis-24x24.png";
    }

    @CheckForNull
    @Override
    public String getDisplayName() {
        return "Hello World";
    }

    @CheckForNull
    @Override
    public String getUrlName() {
        return "warnings-ng-aggregate";
    }
}
