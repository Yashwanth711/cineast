package org.vitrivr.cineast.core.data.distance;

import org.vitrivr.cineast.core.data.CorrespondenceFunction;
import org.vitrivr.cineast.core.data.score.SegmentScoreElement;

public class SegmentDistanceElement extends AbstractDistanceElement {
  public SegmentDistanceElement(String id, double distance) {
    super(id, distance);
  }

  public String getSegmentId() {
    return this.getId();
  }

  @Override
  public SegmentScoreElement toScore(CorrespondenceFunction f) {
    double score = f.applyAsDouble(this.getDistance());
    return new SegmentScoreElement(this.getSegmentId(), score);
  }
}
