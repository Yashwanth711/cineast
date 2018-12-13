package org.vitrivr.cineast.core.features;

import org.vitrivr.cineast.core.config.ReadableQueryConfig;
import org.vitrivr.cineast.core.data.segments.SegmentContainer;
import org.vitrivr.cineast.core.features.abstracts.SolrTextRetriever;

public class TagsFtSearch extends SolrTextRetriever {

  public static final String TAGS_FT_TABLE_NAME = "features_tagsft";

  /**
   * Default constructor for {@link TagsFtSearch}.
   */
  public TagsFtSearch() {
    super(TagsFtSearch.TAGS_FT_TABLE_NAME);
  }

  @Override
  protected String[] generateQuery(SegmentContainer sc, ReadableQueryConfig qc) {
    return sc.getText().split(" ");
  }
}