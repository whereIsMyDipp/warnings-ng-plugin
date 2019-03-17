package io.jenkins.plugins.analysis.core.charts;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;

import org.eclipse.collections.api.list.ImmutableList;

import edu.hm.hafner.analysis.Severity;
import edu.umd.cs.findbugs.annotations.NonNull;

import hudson.model.Run;

import io.jenkins.plugins.analysis.core.util.AnalysisBuild;
import io.jenkins.plugins.analysis.core.util.QualityGateStatus;
import io.jenkins.plugins.analysis.core.util.StaticAnalysisRun;

/**
 * FIXME: comment class.
 *
 * @author Ullrich Hafner
 */
public class CompositeResult implements Iterable<StaticAnalysisRun> {
    private final Collection<StaticAnalysisRun> results;

    public CompositeResult(final List<Iterable<StaticAnalysisRun>> results) {
        SortedMap<AnalysisBuild, StaticAnalysisRun> resultsByBuild = new TreeMap<>();
        for (Iterable<StaticAnalysisRun> toolResults : results) {
            for (StaticAnalysisRun analysisRun : toolResults) {
                resultsByBuild.merge(analysisRun.getBuild(), analysisRun, CompositeStaticAnalysisRun::new);
            }
        }
        this.results = resultsByBuild.values();
    }

    @NonNull
    @Override
    public Iterator<StaticAnalysisRun> iterator() {
        return results.iterator();
    }

    static class CompositeStaticAnalysisRun implements StaticAnalysisRun {
        private final StaticAnalysisRun first;
        private final StaticAnalysisRun second;

        public CompositeStaticAnalysisRun(final StaticAnalysisRun first, final StaticAnalysisRun second) {
            this.first = first;
            this.second = second;
        }

        @Override
        public Run<?, ?> getOwner() {
            return null;
        }

        @Override
        public ImmutableList<String> getErrorMessages() {
            return null;
        }

        @Override
        public ImmutableList<String> getInfoMessages() {
            return null;
        }

        @Override
        public int getSuccessfulSinceBuild() {
            return 0;
        }

        @Override
        public QualityGateStatus getQualityGateStatus() {
            return null;
        }

        @Override
        public Optional<Run<?, ?>> getReferenceBuild() {
            return Optional.empty();
        }

        @Override
        public Map<String, Integer> getSizePerOrigin() {
            return null;
        }

        @Override
        public AnalysisBuild getBuild() {
            return first.getBuild();
        }

        @Override
        public int getNoIssuesSinceBuild() {
            return 0;
        }

        @Override
        public int getFixedSize() {
            return 0;
        }

        @Override
        public int getTotalSize() {
            return 0;
        }

        @Override
        public int getTotalSizeOf(final Severity severity) {
            return first.getTotalSizeOf(severity) + second.getTotalSizeOf(severity);
        }

        @Override
        public int getNewSize() {
            return 0;
        }

        @Override
        public int getNewSizeOf(final Severity severity) {
            return 0;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            CompositeStaticAnalysisRun that = (CompositeStaticAnalysisRun) o;
            return first.equals(that.first) && second.equals(that.second);
        }

        @Override
        public int hashCode() {
            return Objects.hash(first, second);
        }
    }
}
