package com.soundcloud.android.discovery;

class ChartsItem extends DiscoveryItem {
    private final Chart newAndHotChart;
    private final Chart topFiftyChart;

    ChartsItem(Chart newAndHotChart, Chart topFiftyChart) {
        super(Kind.ChartItem);
        this.newAndHotChart = newAndHotChart;
        this.topFiftyChart = topFiftyChart;
    }

    Chart getNewAndHotChart() {
        return newAndHotChart;
    }

    Chart getTopFiftyChart() {
        return topFiftyChart;
    }
}
