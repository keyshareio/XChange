package org.knowm.xchange.bleutrade;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.knowm.xchange.bleutrade.dto.account.BleutradeBalance;
import org.knowm.xchange.bleutrade.dto.marketdata.BleutradeCurrency;
import org.knowm.xchange.bleutrade.dto.marketdata.BleutradeLevel;
import org.knowm.xchange.bleutrade.dto.marketdata.BleutradeMarket;
import org.knowm.xchange.bleutrade.dto.marketdata.BleutradeMarketsReturn;
import org.knowm.xchange.bleutrade.dto.marketdata.BleutradeOrderBook;
import org.knowm.xchange.bleutrade.dto.marketdata.BleutradeTicker;
import org.knowm.xchange.bleutrade.dto.marketdata.BleutradeTrade;
import org.knowm.xchange.bleutrade.dto.trade.BleutradeOpenOrder;
import org.knowm.xchange.currency.Currency;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.Order.OrderType;
import org.knowm.xchange.dto.account.Balance;
import org.knowm.xchange.dto.account.Wallet;
import org.knowm.xchange.dto.marketdata.OrderBook;
import org.knowm.xchange.dto.marketdata.Ticker;
import org.knowm.xchange.dto.marketdata.Trade;
import org.knowm.xchange.dto.marketdata.Trades;
import org.knowm.xchange.dto.marketdata.Trades.TradeSortType;
import org.knowm.xchange.dto.meta.CurrencyMetaData;
import org.knowm.xchange.dto.meta.ExchangeMetaData;
import org.knowm.xchange.dto.meta.MarketMetaData;
import org.knowm.xchange.dto.trade.LimitOrder;
import org.knowm.xchange.dto.trade.OpenOrders;
import org.knowm.xchange.utils.jackson.CurrencyPairDeserializer;

public class BleutradeAdapters {

  public static Set<CurrencyPair> adaptBleutradeCurrencyPairs(BleutradeMarketsReturn response) {

    List<BleutradeMarket> markets = response.getResult();
    Set<CurrencyPair> currencyPairs = new HashSet<CurrencyPair>();

    for (BleutradeMarket market : markets) {
      currencyPairs.add(BleutradeUtils.toCurrencyPair(market.getMarketName()));
    }

    return currencyPairs;
  }

  public static Ticker adaptBleutradeTicker(BleutradeTicker bleutradeTicker) {

    Ticker.Builder builder = new Ticker.Builder();
    builder.ask(bleutradeTicker.getAsk());
    builder.bid(bleutradeTicker.getBid());
    builder.currencyPair(BleutradeUtils.toCurrencyPair(bleutradeTicker.getMarketName()));
    builder.high(bleutradeTicker.getHigh());
    builder.last(bleutradeTicker.getLast());
    builder.low(bleutradeTicker.getLow());
    builder.timestamp(BleutradeUtils.toDate(bleutradeTicker.getTimeStamp()));
    builder.volume(bleutradeTicker.getVolume());
    builder.vwap(bleutradeTicker.getAverage());

    return builder.build();
  }

  public static OrderBook adaptBleutradeOrderBook(BleutradeOrderBook bleutradeOrderBook, CurrencyPair currencyPair) {

    List<BleutradeLevel> bleutradeAsks = bleutradeOrderBook.getSell();
    List<BleutradeLevel> bleutradeBids = bleutradeOrderBook.getBuy();

    List<LimitOrder> asks = new ArrayList<LimitOrder>();
    List<LimitOrder> bids = new ArrayList<LimitOrder>();

    for (BleutradeLevel ask : bleutradeAsks) {

      LimitOrder.Builder builder = new LimitOrder.Builder(OrderType.ASK, currencyPair);
      builder.limitPrice(ask.getRate());
      builder.tradableAmount(ask.getQuantity());
      asks.add(builder.build());
    }

    for (BleutradeLevel bid : bleutradeBids) {

      LimitOrder.Builder builder = new LimitOrder.Builder(OrderType.BID, currencyPair);
      builder.limitPrice(bid.getRate());
      builder.tradableAmount(bid.getQuantity());
      bids.add(builder.build());
    }

    return new OrderBook(null, asks, bids);
  }

  public static Trades adaptBleutradeMarketHistory(List<BleutradeTrade> bleutradeTrades, CurrencyPair currencyPair) {

    List<Trade> trades = new ArrayList<Trade>();

    for (BleutradeTrade bleutradeTrade : bleutradeTrades) {

      Trade.Builder builder = new Trade.Builder();
      builder.currencyPair(currencyPair);
      builder.price(bleutradeTrade.getPrice());
      builder.timestamp(BleutradeUtils.toDate(bleutradeTrade.getTimeStamp()));
      builder.tradableAmount(bleutradeTrade.getQuantity());
      builder.type(bleutradeTrade.getOrderType().equals("BUY") ? OrderType.BID : OrderType.ASK);
      trades.add(builder.build());
    }

    return new Trades(trades, TradeSortType.SortByTimestamp);
  }

  public static Wallet adaptBleutradeBalances(List<BleutradeBalance> bleutradeBalances) {

    List<Balance> balances = new ArrayList<Balance>();

    for (BleutradeBalance bleutradeBalance : bleutradeBalances) {
      balances.add(new Balance(Currency.getInstance(bleutradeBalance.getCurrency()), bleutradeBalance.getBalance(), bleutradeBalance.getAvailable(),
          bleutradeBalance.getPending()));

    }

    return new Wallet(null, balances);
  }

  public static OpenOrders adaptBleutradeOpenOrders(List<BleutradeOpenOrder> bleutradeOpenOrders) {

    List<LimitOrder> openOrders = new ArrayList<LimitOrder>();

    for (BleutradeOpenOrder bleuTradeOpenOrder : bleutradeOpenOrders) {

      OrderType type = bleuTradeOpenOrder.getType().equals("BUY") ? OrderType.BID : OrderType.ASK;
      CurrencyPair currencyPair = BleutradeUtils.toCurrencyPair(bleuTradeOpenOrder.getExchange());

      LimitOrder.Builder builder = new LimitOrder.Builder(type, currencyPair);
      builder.id(bleuTradeOpenOrder.getOrderId());
      builder.limitPrice(bleuTradeOpenOrder.getPrice());
      builder.tradableAmount(bleuTradeOpenOrder.getQuantityRemaining());
      openOrders.add(builder.build());
    }

    return new OpenOrders(openOrders);
  }

  public static ExchangeMetaData adaptToExchangeMetaData(List<BleutradeCurrency> bleutradeCurrencies, List<BleutradeMarket> bleutradeMarkets) {

    Map<CurrencyPair, MarketMetaData> marketMetaDataMap = new HashMap<CurrencyPair, MarketMetaData>();
    Map<Currency, CurrencyMetaData> currencyMetaDataMap = new HashMap<Currency, CurrencyMetaData>();

    for (BleutradeCurrency bleutradeCurrency : bleutradeCurrencies) {
      // the getTxFee parameter is the withdrawal charge in the currency in question
      currencyMetaDataMap.put(Currency.getInstance(bleutradeCurrency.getCurrency()), new CurrencyMetaData(8));
    }

    // https://bleutrade.com/help/fees_and_deadlines 11/25/2015 all == 0.25%
    BigDecimal txFee = new BigDecimal("0.0025");

    for (BleutradeMarket bleutradeMarket : bleutradeMarkets) {
      CurrencyPair currencyPair = CurrencyPairDeserializer.getCurrencyPairFromString(bleutradeMarket.getMarketName());
      MarketMetaData marketMetaData = new MarketMetaData(txFee, bleutradeMarket.getMinTradeSize(), 8);
      marketMetaDataMap.put(currencyPair, marketMetaData);
    }

    return new ExchangeMetaData(marketMetaDataMap, currencyMetaDataMap, null, null, null);
  }

}
