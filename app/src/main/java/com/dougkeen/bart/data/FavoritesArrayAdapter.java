package com.dougkeen.bart.data;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.RelativeLayout;
import android.widget.TextSwitcher;
import android.widget.TextView;
import android.widget.ViewSwitcher.ViewFactory;

import com.dougkeen.bart.R;
import com.dougkeen.bart.controls.CountdownTextView;
import com.dougkeen.bart.controls.TimedTextSwitcher;
import com.dougkeen.bart.model.Departure;
import com.dougkeen.bart.model.StationPair;
import com.dougkeen.bart.model.TextProvider;
import com.dougkeen.bart.services.EtdService;
import com.dougkeen.bart.services.EtdService.EtdServiceBinder;
import com.dougkeen.bart.services.EtdService.EtdServiceListener;
import com.dougkeen.bart.services.EtdService_;

public class FavoritesArrayAdapter extends ArrayAdapter<StationPair> {

    private boolean mBound = false;

    private EtdService mEtdService;

    private Activity mHostActivity;

    private Map<StationPair, EtdListener> mEtdListeners = new HashMap<StationPair, EtdListener>();

    private final ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceDisconnected(ComponentName name) {
            mEtdService = null;
            mBound = false;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mEtdService = ((EtdServiceBinder) service).getService();
            mBound = true;
            if (!isEmpty()) {
                setUpEtdListeners();
            }
        }
    };

    public void setUpEtdListeners() {
        if (mBound && mEtdService != null) {
            for (int i = getCount() - 1; i >= 0; i--) {
                final StationPair item = getItem(i);
                mEtdListeners.put(item, new EtdListener(item, mEtdService));
            }
        }
    }

    public void clearEtdListeners() {
        if (mBound && mEtdService != null) {
            for (EtdListener listener : mEtdListeners.values()) {
                listener.close(mEtdService);
            }
            mEtdListeners.clear();
        }
    }

    public boolean areEtdListenersActive() {
        return !mEtdListeners.isEmpty();
    }

    public FavoritesArrayAdapter(Context context, int textViewResourceId,
                                 List<StationPair> objects) {
        super(context, textViewResourceId, objects);
        mHostActivity = (Activity) context;
        mHostActivity.bindService(EtdService_.intent(mHostActivity).get(),
                mConnection, Context.BIND_AUTO_CREATE);
    }

    public void close() {
        if (mBound) {
            mHostActivity.unbindService(mConnection);
        }
    }

    @Override
    public void add(StationPair object) {
        super.add(object);
        if (mEtdService != null && mBound) {
            mEtdListeners.put(object, new EtdListener(object, mEtdService));
        }
    }

    @Override
    public void remove(StationPair object) {
        super.remove(object);
        if (mEtdListeners.containsKey(object) && mEtdService != null & mBound) {
            mEtdListeners.get(object).close(mEtdService);
            mEtdListeners.remove(object);
        }
    }

    public void move(StationPair object, int to) {
        super.remove(object);
        super.insert(object, to);
    }

    @Override
    public void clear() {
        super.clear();
        clearEtdListeners();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view;
        if (convertView != null && convertView instanceof RelativeLayout) {
            view = convertView;
        } else {
            LayoutInflater inflater = LayoutInflater.from(getContext());
            view = inflater.inflate(R.layout.favorite_listing, parent, false);
        }

        final StationPair pair = getItem(position);

        final EtdListener etdListener = mEtdListeners.get(pair);

        final TimedTextSwitcher uncertaintyTextSwitcher = (TimedTextSwitcher) view
                .findViewById(R.id.uncertainty);
        initTextSwitcher(uncertaintyTextSwitcher);

        if (etdListener == null || etdListener.getFirstDeparture() == null) {
            uncertaintyTextSwitcher.setCurrentText(pair.getFare());
        } else {
            CountdownTextView countdownTextView = (CountdownTextView) view
                    .findViewById(R.id.countdownText);
            countdownTextView.setText(etdListener.getFirstDeparture()
                    .getCountdownText());
            countdownTextView.setTextProvider(new TextProvider() {
                @Override
                public String getText(long tickNumber) {
                    return etdListener.getFirstDeparture().getCountdownText();
                }
            });

            final String uncertaintyText = etdListener.getFirstDeparture()
                    .getUncertaintyText();
            if (!StringUtils.isBlank(uncertaintyText)) {
                uncertaintyTextSwitcher.setCurrentText(uncertaintyText);
            } else {
                uncertaintyTextSwitcher.setCurrentText(pair.getFare());
            }
            uncertaintyTextSwitcher.setTextProvider(new TextProvider() {
                @Override
                public String getText(long tickNumber) {
                    final String arrive = etdListener.getFirstDeparture()
                            .getEstimatedArrivalTimeText(getContext(), true);
                    int mod = StringUtils.isNotBlank(arrive) ? 8 : 6;
                    if (tickNumber % mod <= 1) {
                        return pair.getFare();
                    } else if (tickNumber % mod <= 3) {
                        return "Dep "
                                + etdListener.getFirstDeparture()
                                .getEstimatedDepartureTimeText(
                                        getContext(), true);
                    } else if (mod == 8 && tickNumber % mod <= 5) {
                        return "Arr " + arrive;
                    } else {
                        return etdListener.getFirstDeparture()
                                .getUncertaintyText();
                    }
                }
            });
        }

        ((TextView) view.findViewById(R.id.originText)).setText(pair
                .getOrigin().name);
        ((TextView) view.findViewById(R.id.destinationText)).setText(pair
                .getDestination().name);

        return view;
    }

    private void initTextSwitcher(TextSwitcher textSwitcher) {
        if (textSwitcher.getInAnimation() == null) {
            textSwitcher.setFactory(new ViewFactory() {
                public View makeView() {
                    return LayoutInflater.from(getContext()).inflate(
                            R.layout.uncertainty_textview, null);
                }
            });

            textSwitcher.setInAnimation(AnimationUtils.loadAnimation(
                    getContext(), android.R.anim.slide_in_left));
            textSwitcher.setOutAnimation(AnimationUtils.loadAnimation(
                    getContext(), android.R.anim.slide_out_right));
        }
    }

    private class EtdListener implements EtdServiceListener {

        private final StationPair mStationPair;

        private Departure firstDeparture;

        protected EtdListener(StationPair mStationPair, EtdService etdService) {
            super();
            this.mStationPair = mStationPair;
            etdService.registerListener(this, true);
        }

        protected void close(EtdService etdService) {
            etdService.unregisterListener(this);
        }

        @Override
        public void onETDChanged(List<Departure> departures) {
            for (Departure departure : departures) {
                if (!departure.hasDeparted()) {
                    if (!departure.equals(firstDeparture)) {
                        firstDeparture = departure;
                        FavoritesArrayAdapter.this.notifyDataSetChanged();
                    }
                    return;
                }
            }
        }

        @Override
        public void onError(String errorMessage) {
        }

        @Override
        public void onRequestStarted() {
        }

        @Override
        public void onRequestEnded() {
        }

        @Override
        public StationPair getStationPair() {
            return mStationPair;
        }

        public Departure getFirstDeparture() {
            return firstDeparture;
        }
    }
}
