package se.sics.kompics.wan.daemon.indexer;

import se.sics.kompics.Init;

public final class IndexerInit extends Init {

	private final long indexingPeriod;
	
	public IndexerInit(long indexingPeriod) {
		super();
		this.indexingPeriod = indexingPeriod;
	}

	public long getIndexingPeriod() {
		return indexingPeriod;
	}

}