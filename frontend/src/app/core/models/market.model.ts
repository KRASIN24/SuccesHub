export interface PriceEntry {
  pair: string;
  price: string;
  change: string;
}

export interface MarketData {
  prices: PriceEntry[];
}
