package io.jenkins.plugins.analysis.core.charts;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import edu.hm.hafner.analysis.Severity;

import io.jenkins.plugins.analysis.core.model.AnalysisResult.BuildProperties;
import io.jenkins.plugins.analysis.core.util.AnalysisBuild;
import io.jenkins.plugins.analysis.core.util.LocalizedSeverity;
import io.jenkins.plugins.analysis.core.util.StaticAnalysisRun;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests the class {@link SeverityTrendChart}.
 *
 * @author Ullrich Hafner
 */
class SeverityTrendChartTest {
    @Test
    void shouldCreatePriorityChartForJobAndAction() {
        SeverityTrendChart chart = new SeverityTrendChart();

        List<StaticAnalysisRun> results = new ArrayList<>();
        results.add(createResult(1, 2, 3, 1));
        results.add(createResult(2, 4, 6, 2));
        
        LinesChartModel model = chart.create(results, new ChartModelConfiguration());

        verifySeries(model.getSeries().get(0), Severity.WARNING_LOW, 3, 6);
        verifySeries(model.getSeries().get(1), Severity.WARNING_NORMAL, 2, 4);
        verifySeries(model.getSeries().get(2), Severity.WARNING_HIGH, 1, 2);

        assertThatJson(model).node("xAxisLabels")
                .isArray().hasSize(2)
                .contains("#1")
                .contains("#2");

        assertThatJson(model).node("series")
                .isArray().hasSize(3);
    }

    @Test
    void shouldCreatePriorityChartForJobAndMultipleActions() {
        SeverityTrendChart chart = new SeverityTrendChart();

        List<StaticAnalysisRun> resultsCheckStyle = new ArrayList<>();
        resultsCheckStyle.add(createResult(1, 2, 3, 1));
        resultsCheckStyle.add(createResult(2, 4, 6, 2));

        List<StaticAnalysisRun> resultsSpotBugs = new ArrayList<>();
        resultsSpotBugs.add(createResult(11, 12, 13, 1));
        resultsSpotBugs.add(createResult(12, 14, 16, 2));

        List<List<? extends StaticAnalysisRun>> jobResults = new ArrayList<>();
        jobResults.add(resultsCheckStyle);
        jobResults.add(resultsSpotBugs);

        // FIXME: finish test
    }

    private void verifySeries(final LineSeries high, final Severity severity,
            final int valueFirstBuild, final int valueSecondBuild) {
        assertThatJson(high).node("name")
                .isEqualTo(LocalizedSeverity.getLocalizedString(severity));
        assertThatJson(high).node("data")
                .isArray().hasSize(2)
                .contains(valueFirstBuild)
                .contains(valueSecondBuild);
    }

    private StaticAnalysisRun createResult(final int high, final int normal, final int low, final int number) {
        StaticAnalysisRun buildResult = mock(StaticAnalysisRun.class);

        when(buildResult.getTotalSizeOf(Severity.WARNING_HIGH)).thenReturn(high);
        when(buildResult.getTotalSizeOf(Severity.WARNING_NORMAL)).thenReturn(normal);
        when(buildResult.getTotalSizeOf(Severity.WARNING_LOW)).thenReturn(low);

        AnalysisBuild build = new BuildProperties(number, "#" + number, 10);
        when(buildResult.getBuild()).thenReturn(build);
        return buildResult;
    }
}