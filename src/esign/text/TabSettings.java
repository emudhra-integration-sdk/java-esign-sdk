package esign.text;

import java.util.ArrayList;
import java.util.List;

public class TabSettings {

    public static final float DEFAULT_TAB_INTERVAL = 36.0F;

    public static TabStop getTabStopNewInstance(float currentPosition, TabSettings tabSettings) {
        if (tabSettings != null) {
            return tabSettings.getTabStopNewInstance(currentPosition);
        }
        return TabStop.newInstance(currentPosition, 36.0F);
    }

    private List<TabStop> tabStops = new ArrayList<TabStop>();
    private float tabInterval = 36.0F;

    public TabSettings() {
    }

    public TabSettings(List<TabStop> tabStops) {
        this.tabStops = tabStops;
    }

    public TabSettings(float tabInterval) {
        this.tabInterval = tabInterval;
    }

    public TabSettings(List<TabStop> tabStops, float tabInterval) {
        this.tabStops = tabStops;
        this.tabInterval = tabInterval;
    }

    public List<TabStop> getTabStops() {
        return this.tabStops;
    }

    public void setTabStops(List<TabStop> tabStops) {
        this.tabStops = tabStops;
    }

    public float getTabInterval() {
        return this.tabInterval;
    }

    public void setTabInterval(float tabInterval) {
        this.tabInterval = tabInterval;
    }

    public TabStop getTabStopNewInstance(float currentPosition) {
        TabStop tabStop = null;
        if (this.tabStops != null) {
            for (TabStop currentTabStop : this.tabStops) {
                if ((currentTabStop.getPosition() - currentPosition) > 0.001D) {
                    tabStop = new TabStop(currentTabStop);

                    break;
                }
            }
        }
        if (tabStop == null) {
            tabStop = TabStop.newInstance(currentPosition, this.tabInterval);
        }

        return tabStop;
    }
}
