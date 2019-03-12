package io.jenkins.plugins.analysis.core.charts;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import edu.hm.hafner.analysis.Severity;

import io.jenkins.plugins.analysis.core.charts.LineSeries.FilledMode;
import io.jenkins.plugins.analysis.core.charts.LineSeries.StackedMode;
import io.jenkins.plugins.analysis.core.model.History;
import io.jenkins.plugins.analysis.core.util.LocalizedSeverity;
import io.jenkins.plugins.analysis.core.util.StaticAnalysisRun;

/**
 * Builds the model for a trend chart showing all issues by severity for a given number of builds.
 *
 * @author Ullrich Hafner
 */
public class SeverityTrendChart implements TrendChart {
    @Override
    public LinesChartModel create(final Iterable<? extends StaticAnalysisRun> results,
            final ChartModelConfiguration configuration) {
        SeveritySeriesBuilder builder = new SeveritySeriesBuilder();
        LinesDataSet dataSet = builder.createDataSet(configuration, results);

        LinesChartModel model = new LinesChartModel();
        model.addXAxisLabels(dataSet.getXAxisLabels());

        Severity[] visibleSeverities
                = {Severity.WARNING_LOW, Severity.WARNING_NORMAL, Severity.WARNING_HIGH, Severity.ERROR};
        for (Severity severity : visibleSeverities) {
            String id = severity.getName();
            if (dataSet.hasSeries(id)) {
                LineSeries series = createSeries(severity);
                series.addAll(dataSet.getSeries(id));
                model.addSeries(series);
            }
        }

        return model;
    }

    public LinesChartModel create(final Collection<Iterable<? extends StaticAnalysisRun>> results,
            final ChartModelConfiguration configuration) {
        SeveritySeriesBuilder builder = new SeveritySeriesBuilder();
        LinesDataSet dataSet = builder.createAggregation(configuration, results);

        LinesChartModel model = new LinesChartModel();
        model.addXAxisLabels(dataSet.getXAxisLabels());

        return model;
    }

    private LineSeries createSeries(final Severity severity) {
        return new LineSeries(LocalizedSeverity.getLocalizedString(severity),
                SeverityPalette.getColor(severity).getNormal(), StackedMode.STACKED, FilledMode.FILLED);
    }
}
