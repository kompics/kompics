/**
 * This file is part of the Kompics P2P Framework.
 * 
 * Copyright (C) 2009 Swedish Institute of Computer Science (SICS)
 * Copyright (C) 2009 Royal Institute of Technology (KTH)
 *
 * Kompics is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package se.sics.kompics.p2p.fd.ping;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.Properties;

import se.sics.kompics.network.Transport;

/**
 * The <code>PingFailureDetectorConfiguration</code> class.
 * 
 * @author Cosmin Arad <cosmin@sics.se>
 * @version $Id$
 */
public final class PingFailureDetectorConfiguration {

	private final long livePeriod;
	private final long suspectedPeriod;
	private final long minRto;
	private final long timeoutPeriodIncrement;
	private final Transport protocol;

	public PingFailureDetectorConfiguration(long livePeriod,
			long suspectedPeriod, long minRto, long timeoutPeriodIncrement,
			Transport protocol) {
		this.livePeriod = livePeriod;
		this.suspectedPeriod = suspectedPeriod;
		this.minRto = minRto;
		this.timeoutPeriodIncrement = timeoutPeriodIncrement;
		this.protocol = protocol;
	}

	public final long getLivePeriod() {
		return livePeriod;
	}

	public final long getSuspectedPeriod() {
		return suspectedPeriod;
	}

	public final long getMinRto() {
		return minRto;
	}

	public final long getTimeoutPeriodIncrement() {
		return timeoutPeriodIncrement;
	}

	public Transport getProtocol() {
		return protocol;
	}

	public void store(String file) throws IOException {
		Properties p = new Properties();
		p.setProperty("live.period", "" + livePeriod);
		p.setProperty("suspected.period", "" + suspectedPeriod);
		p.setProperty("rto.min", "" + minRto);
		p.setProperty("timeout.period.increment", "" + timeoutPeriodIncrement);
		p.setProperty("transport.protocol", protocol.name());

		Writer writer = new FileWriter(file);
		p.store(writer, "se.sics.kompics.p2p.fd.ping");
	}

	public static PingFailureDetectorConfiguration load(String file)
			throws IOException {
		Properties p = new Properties();
		Reader reader = new FileReader(file);
		p.load(reader);

		long livePeriod = Long.parseLong(p.getProperty("live.period"));
		long suspectedPeriod = Long
				.parseLong(p.getProperty("suspected.period"));
		long minRto = Long.parseLong(p.getProperty("rto.min"));
		long timeoutPeriodIncrement = Long.parseLong(p
				.getProperty("timeout.period.increment"));
		Transport protocol = Enum.valueOf(Transport.class, p
				.getProperty("transport.protocol"));

		return new PingFailureDetectorConfiguration(livePeriod,
				suspectedPeriod, minRto, timeoutPeriodIncrement, protocol);
	}
}
