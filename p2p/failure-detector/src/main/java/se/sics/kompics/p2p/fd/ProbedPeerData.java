package se.sics.kompics.p2p.fd;

public class ProbedPeerData {

	public double avgRTT;
	public double varRTT;
	public double rtto;
	public double showedRtto;
	public double rttoMin;

	public ProbedPeerData(double avgRTT, double varRTT, double rtto,
			double showedRtto, double rttoMin) {
		super();
		this.avgRTT = avgRTT;
		this.varRTT = varRTT;
		this.rtto = rtto;
		this.showedRtto = showedRtto;
		this.rttoMin = rttoMin;
	}
}
