package ch.unibas.cs.dbis.cineast.core.features.abstracts;

import java.util.List;

import ch.unibas.cs.dbis.cineast.core.config.Config;
import ch.unibas.cs.dbis.cineast.core.config.QueryConfig;
import ch.unibas.cs.dbis.cineast.core.data.ReadableFloatVector;
import ch.unibas.cs.dbis.cineast.core.data.StringDoublePair;
import ch.unibas.cs.dbis.cineast.core.db.PersistencyWriter;
import ch.unibas.cs.dbis.cineast.core.db.PersistentTuple;
import ch.unibas.cs.dbis.cineast.core.features.extractor.Extractor;
import ch.unibas.cs.dbis.cineast.core.util.MathHelper;

public abstract class SubDivMotionHistogram extends MotionHistogramCalculator implements Extractor {

protected PersistencyWriter phandler;
	
	protected SubDivMotionHistogram(String tableName, double maxDist){
		super(tableName, (float)maxDist);
	}

	@Override
	public void init(PersistencyWriter<?> phandler) {
		this.phandler = phandler;
		this.phandler.open(this.tableName);
		this.phandler.setFieldNames("id", "hist", "sums");
	}
	
	protected void persist(String shotId, ReadableFloatVector fs1, ReadableFloatVector fs2) {
		PersistentTuple tuple = this.phandler.generateTuple(shotId, fs1, fs2); //FIXME currently only one vector is supported
		this.phandler.persist(tuple);
	}
	
	/**
	 * helper function to retrieve elements close to a vector which has to be generated by the feature module
	 */
	protected List<StringDoublePair> getSimilar(float[] vector, QueryConfig qc) {
		List<StringDoublePair> distances = this.selector.getNearestNeighbours(Config.getRetrieverConfig().getMaxResultsPerModule(), vector, "hist", qc);
		for(StringDoublePair sdp : distances){
			double dist = sdp.value;
			sdp.value = MathHelper.getScore(dist, maxDist);
		}
		return distances;
	}
	
	@Override
	public void finish() {
		if(this.phandler != null){
			this.phandler.close();
		}
		super.finish();
	}
}
