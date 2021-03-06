package org.knowm.xchange.virtex.v2.service.polling;

import org.knowm.xchange.Exchange;
import org.knowm.xchange.service.BaseExchangeService;
import org.knowm.xchange.service.polling.BasePollingService;

/**
 * @author timmolter
 */
public class VirtexBasePollingService extends BaseExchangeService implements BasePollingService {

  /**
   * Constructor
   *
   * @param exchange
   */
  public VirtexBasePollingService(Exchange exchange) {

    super(exchange);
  }
}
