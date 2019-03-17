package io.jenkins.plugins.analysis.core.charts;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import edu.hm.hafner.analysis.Severity;

import io.jenkins.plugins.analysis.core.charts.CompositeResult.CompositeStaticAnalysisRun;
import io.jenkins.plugins.analysis.core.model.AnalysisResult.BuildProperties;
import io.jenkins.plugins.analysis.core.util.AnalysisBuild;
import io.jenkins.plugins.analysis.core.util.StaticAnalysisRun;

import static io.jenkins.plugins.analysis.core.assertions.Assertions.*;
import static java.util.Arrays.*;
import static org.mockito.Mockito.*;

/**
 * Tests the class {@link CompositeResult}.
 *
 * @author Ullrich Hafner
 */
class CompositeResultTest {
    @Test
    void shouldCreateResultOfSequenceWithIdenticalBuilds() {
        List<StaticAnalysisRun> resultsCheckStyle = new ArrayList<>();
        resultsCheckStyle.add(createResult(1, 2, 3, 1));
        resultsCheckStyle.add(createResult(2, 4, 6, 2));

        List<StaticAnalysisRun> resultsSpotBugs = new ArrayList<>();
        resultsSpotBugs.add(createResult(11, 12, 13, 1));
        resultsSpotBugs.add(createResult(12, 14, 16, 2));

        CompositeResult compositeResult = new CompositeResult(asList(resultsCheckStyle, resultsSpotBugs));

        assertThat(compositeResult.iterator()).toIterable().hasSize(2);

        Iterator<StaticAnalysisRun> iterator = compositeResult.iterator();
        StaticAnalysisRun first = iterator.next();
        StaticAnalysisRun second = iterator.next();

        assertThat(first).hasBuild(createBuild(1));
        assertThat(second).hasBuild(createBuild(2));

        assertThat(first.getTotalSizeOf(Severity.WARNING_HIGH)).isEqualTo(12);
        assertThat(first.getTotalSizeOf(Severity.WARNING_NORMAL)).isEqualTo(14);
        assertThat(first.getTotalSizeOf(Severity.WARNING_LOW)).isEqualTo(16);

        assertThat(second.getTotalSizeOf(Severity.WARNING_HIGH)).isEqualTo(14);
        assertThat(second.getTotalSizeOf(Severity.WARNING_NORMAL)).isEqualTo(18);
        assertThat(second.getTotalSizeOf(Severity.WARNING_LOW)).isEqualTo(22);
    }

    private StaticAnalysisRun createResult(final int high, final int normal, final int low, final int number) {
        StaticAnalysisRun buildResult = mock(StaticAnalysisRun.class);

        when(buildResult.getTotalSizeOf(Severity.WARNING_HIGH)).thenReturn(high);
        when(buildResult.getTotalSizeOf(Severity.WARNING_NORMAL)).thenReturn(normal);
        when(buildResult.getTotalSizeOf(Severity.WARNING_LOW)).thenReturn(low);

        AnalysisBuild build = createBuild(number);
        when(buildResult.getBuild()).thenReturn(build);
        return buildResult;
    }

    private AnalysisBuild createBuild(final int number) {
        return new BuildProperties(number, "#" + number, 10);
    }

    /**
     * Tests the class {@link CompositeStaticAnalysisRun}.
     */
    @Nested
    class CompositeStaticAnalysisRunTest {
        @Test
        void shouldTestMerge() {
            StaticAnalysisRun first = createResult(1, 2, 3, 1);
            StaticAnalysisRun second = createResult(4, 5, 6, 1);

            CompositeStaticAnalysisRun run = new CompositeStaticAnalysisRun(first, second);

            assertThat(run).hasBuild(createBuild(1));
            assertThat(run.getTotalSizeOf(Severity.WARNING_HIGH)).isEqualTo(5);
            assertThat(run.getTotalSizeOf(Severity.WARNING_NORMAL)).isEqualTo(7);
            assertThat(run.getTotalSizeOf(Severity.WARNING_LOW)).isEqualTo(9);
        }
    }
}