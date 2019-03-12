package io.jenkins.plugins.analysis.core.charts;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.multimap.list.FastListMultimap;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import edu.hm.hafner.util.VisibleForTesting;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import io.jenkins.plugins.analysis.core.charts.ChartModelConfiguration.AxisType;
import io.jenkins.plugins.analysis.core.model.AnalysisHistory;
import io.jenkins.plugins.analysis.core.util.AnalysisBuild;
import io.jenkins.plugins.analysis.core.util.StaticAnalysisRun;

import static java.util.stream.Collectors.*;

/**
 * Provides the base algorithms to create a data set for a static analysis graph. The actual series for each result
 * needs to be implemented by sub classes in method {@link #computeSeries}.
 *
 * @author Ullrich Hafner
 */
public abstract class SeriesBuilder {
    private final ResultTime resultTime;

    /**
     * Creates a new {@link SeriesBuilder}.
     */
    SeriesBuilder() {
        this(new ResultTime());
    }

    @VisibleForTesting
    SeriesBuilder(final ResultTime resultTime) {
        this.resultTime = resultTime;
    }

    /**
     * Creates a new data set for a category graph from the specified static analysis results. The results (provided by
     * an iterator) must be sorted by build number in descending order. I.e., the iterator starts with the newest build
     * and stops at the oldest build. The actual series for each result needs to be implemented by sub classes by
     * overriding method {@link #computeSeries}.
     *
     * @param configuration
     *         configures the data set (how many results should be process, etc.)
     * @param results
     *         the ordered static analysis results
     *
     * @return the created data set
     */
    public LinesDataSet createDataSet(final ChartModelConfiguration configuration,
            final Iterable<? extends StaticAnalysisRun> results) {
        SortedMap<AnalysisBuild, Map<String, Integer>> seriesPerBuild = createSeriesPerBuild(configuration, results);

        if (configuration.getAxisType() == AxisType.BUILD) {
            return createDataSetPerBuildNumber(seriesPerBuild);
        }
        else {
            return createDataSetPerDay(averageByDate(seriesPerBuild));
        }
    }

    @SuppressWarnings("rawtypes")
    private SortedMap<AnalysisBuild, Map<String, Integer>> createSeriesPerBuild(
            final ChartModelConfiguration configuration, final Iterable<? extends StaticAnalysisRun> results) {
        int buildCount = 0;
        SortedMap<AnalysisBuild, Map<String, Integer>> valuesPerBuildNumber = new TreeMap<>();
        for (StaticAnalysisRun current : results) {
            if (resultTime.isResultTooOld(configuration, current)) {
                break;
            }
            Map<String, Integer> series = computeSeries(current);
            if (!series.isEmpty()) {
                valuesPerBuildNumber.put(current.getBuild(), series);
            }

            if (configuration.isBuildCountDefined()) {
                buildCount++;
                if (buildCount >= configuration.getBuildCount()) {
                    break;
                }
            }
        }
        fillMissingValues(valuesPerBuildNumber);

        return valuesPerBuildNumber;
    }

    private void fillMissingValues(final SortedMap<AnalysisBuild, Map<String, Integer>> valuesPerBuildNumber) {
        Set<String> dataSets = valuesPerBuildNumber.values()
                .stream()
                .flatMap(values -> Stream.of(values.keySet()))
                .flatMap(Set::stream)
                .collect(toSet());

        for (Map<String, Integer> series : valuesPerBuildNumber.values()) {
            dataSets.forEach(dataSet -> series.putIfAbsent(dataSet, 0));
        }
    }

    /**
     * Returns the series to plot for the specified build result.
     *
     * @param current
     *         the current build result
     *
     * @return the series to plot
     */
    protected abstract Map<String, Integer> computeSeries(StaticAnalysisRun current);

    /**
     * Creates a data set that contains a series per build number.
     *
     * @param valuesPerBuild
     *         the collected values
     *
     * @return a data set
     */
    private LinesDataSet createDataSetPerBuildNumber(final SortedMap<AnalysisBuild, Map<String, Integer>> valuesPerBuild) {
        LinesDataSet model = new LinesDataSet();
        for (Entry<AnalysisBuild, Map<String, Integer>> series : valuesPerBuild.entrySet()) {
            model.add(series.getKey().getDisplayName(), series.getValue());
        }
        return model;
    }

    /**
     * Creates a data set that contains one series of values per day.
     *
     * @param averagePerDay
     *         the collected values averaged by day
     *
     * @return a data set
     */
    private LinesDataSet createDataSetPerDay(final SortedMap<LocalDate, Map<String, Integer>> averagePerDay) {
        LinesDataSet model = new LinesDataSet();
        for (Entry<LocalDate, Map<String, Integer>> series : averagePerDay.entrySet()) {
            String label = new LocalDateLabel(series.getKey()).toString();
            model.add(label, series.getValue());
        }
        return model;
    }

    /**
     * Aggregates multiple series per day to one single series per day by computing the average value.
     *
     * @param multiSeriesPerDate
     *         the values given as multiple series per day
     *
     * @return the values as one series per day (average)
     */
    private SortedMap<LocalDate, Map<String, Integer>> createSeriesPerDay(
            final FastListMultimap<LocalDate, Map<String, Integer>> multiSeriesPerDate) {
        SortedMap<LocalDate, Map<String, Integer>> seriesPerDate = new TreeMap<>();

        for (LocalDate date : multiSeriesPerDate.keySet()) {
            MutableList<Map<String, Integer>> seriesPerDay = multiSeriesPerDate.get(date);

            Map<String, Integer> mapOfDay =
                    seriesPerDay.stream()
                            .flatMap(m -> m.entrySet().stream())
                            .collect(groupingBy(Map.Entry::getKey, summingInt(Map.Entry::getValue)));
            Map<String, Integer> averagePerDay =
                    mapOfDay.entrySet().stream()
                            .collect(Collectors.toMap(Entry::getKey, e-> e.getValue() / seriesPerDay.size()));
            seriesPerDate.put(date, averagePerDay);
        }
        return seriesPerDate;
    }

    /**
     * Aggregates the series per build to a series per date.
     *
     * @param valuesPerBuild
     *         the series per build
     *
     * @return the series per date
     */
    private SortedMap<LocalDate, Map<String, Integer>> averageByDate(
            final SortedMap<AnalysisBuild, Map<String, Integer>> valuesPerBuild) {
        return createSeriesPerDay(createMultiSeriesPerDay(valuesPerBuild));
    }

    /**
     * Creates a mapping of values per day.
     *
     * @param valuesPerBuild
     *         the values per build
     *
     * @return the multi map with the values per day
     */
    @SuppressFBWarnings("WMI")
    private FastListMultimap<LocalDate, Map<String, Integer>> createMultiSeriesPerDay(
            final Map<AnalysisBuild, Map<String, Integer>> valuesPerBuild) {
        FastListMultimap<LocalDate, Map<String, Integer>> valuesPerDate = FastListMultimap.newMultimap();
        for (AnalysisBuild build : valuesPerBuild.keySet()) {
            LocalDate buildDate = Instant.ofEpochMilli(build.getTimeInMillis())
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate();
            valuesPerDate.put(buildDate, valuesPerBuild.get(build));
        }
        return valuesPerDate;
    }

    public LinesDataSet createAggregation(final ChartModelConfiguration configuration,
            final List<List<? extends StaticAnalysisRun>> results) {
        Set<AnalysisBuild> availableBuilds = Sets.newHashSet();
        Map<String, SortedMap<AnalysisBuild, Map<String, Integer>>> resultsPerTool = new HashMap<>();

        for (Iterable<? extends StaticAnalysisRun> jobResults : results) {
            SortedMap<AnalysisBuild, Map<String, Integer>> toolSeries
                    = createSeriesPerBuild(configuration, jobResults);
            availableBuilds.addAll(toolSeries.keySet());
//            resultsPerTool.put(id, toolSeries);
        }

        // TODO: map from tool ID ->
        // Create results for missing build#

        SortedMap<LocalDate, Map<String, Integer>> result = createSeriesPerDay(null);
        return createDataSetPerDay(result);
    }


    /**
     * Creates an aggregated data set.
     *
     * @param configuration
     *         configures the data set (how many results should be process, etc.)
     * @param resultsOfJobs
     *         the static analysis results of a collection of jobs
     * @return the aggregated data set
     */
    public LinesDataSet createAggregation(final ChartModelConfiguration configuration,
            final Collection<Iterable<? extends StaticAnalysisRun>> resultsOfJobs) {
        Set<LocalDate> availableDates = Sets.newHashSet();
        List<Map<LocalDate, Map<String, Integer>>> averagesPerJob = new ArrayList<>();

        for (Iterable<? extends StaticAnalysisRun> jobResults : resultsOfJobs) {
            Map<LocalDate, Map<String, Integer>> averageByDate = averageByDate(
                    createSeriesPerBuild(configuration, jobResults));
            availableDates.addAll(averageByDate.keySet());
            averagesPerJob.add(averageByDate);
        }

        List<LocalDate> sortedDates = Lists.newArrayList(availableDates);
        Collections.sort(sortedDates);


        for (Map<LocalDate, Map<String, Integer>> averageByDate : averagesPerJob) {
            Map<String, Integer> previous = new HashMap<>();
            for (LocalDate date : sortedDates) {
                averageByDate.putIfAbsent(date, previous);

                previous = averageByDate.get(date);
            }
        }

        FastListMultimap<LocalDate, Map<String, Integer>> valuesPerDate = FastListMultimap.newMultimap();
        for (Map<LocalDate, Map<String, Integer>> averageByDate : averagesPerJob) {
            for (Entry<LocalDate, Map<String, Integer>> entry : averageByDate.entrySet()) {
                valuesPerDate.put(entry.getKey(), entry.getValue());
            }
        }

        SortedMap<LocalDate, Map<String, Integer>> result = createSeriesPerDay(valuesPerDate);
        return createDataSetPerDay(result);
    }

}
