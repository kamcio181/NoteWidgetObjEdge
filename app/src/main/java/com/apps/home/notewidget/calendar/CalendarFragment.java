package com.apps.home.notewidget.calendar;

import android.content.Context;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.apps.home.notewidget.R;
import com.apps.home.notewidget.objects.Event;
import com.apps.home.notewidget.utils.DatabaseHelper;
import com.apps.home.notewidget.utils.Utils;

import java.util.ArrayList;
import java.util.Calendar;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link OnEventClickListener} interface
 * to handle interaction events.
 */
public class CalendarFragment extends Fragment {
    private static final String TAG = CalendarFragment.class.getSimpleName();
    private Context context;
    private RecyclerView recyclerView;
    private int[][] dates;

    private OnEventClickListener mListener;

    public CalendarFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_calendar, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        context = getActivity();
        recyclerView = (RecyclerView) view;

        Calendar calendar = Calendar.getInstance();
        int buffer = 3;
        dates = new int[buffer][3];
        int halfOfBuffer = (buffer -1)/2;
        int dayMillis = 1000*60*60*24;
        calendar.setTimeInMillis(calendar.getTimeInMillis()-(dayMillis*halfOfBuffer));
        for(int i = 0; i<buffer; i++) {
            dates[i] = new int[]{calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)};
            calendar.setTimeInMillis(calendar.getTimeInMillis() + dayMillis);
        }
        recyclerView.setLayoutManager(new LinearLayoutManager(context));
        recyclerView.setAdapter(new CalendarRecyclerViewAdapter(context, dates, mListener));
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnEventClickListener) {
            mListener = (OnEventClickListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnEventClickListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public interface OnEventClickListener {
        void onEventClicked(long id);
    }

    class CalendarRecyclerViewAdapter extends RecyclerView.Adapter<CalendarRecyclerViewAdapter.DayViewHolder>{
        private Context context;
        private int[][] dates;
        private OnEventClickListener listener;
        private DatabaseHelper helper;
        private Calendar calendar = Calendar.getInstance();
        private ArrayList[] allEvents;
        private int[] currentDate;
        private int activeColor;
        private int inactiveColor;

        CalendarRecyclerViewAdapter(Context context, int[][] dates, OnEventClickListener listener) {
            this.context = context;
            this.dates = dates;
            this.listener = listener;
            helper = new DatabaseHelper(context);
            currentDate = new int[]{calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)};
            calendar.set(0,0,0,0,0,0);
            allEvents = new ArrayList[dates.length];
            activeColor = ContextCompat.getColor(context, R.color.colorPrimary);
            inactiveColor = Color.BLACK;
        }

        @Override
        public CalendarRecyclerViewAdapter.DayViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new DayViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.day_item, parent, false));
        }

        @Override
        public void onBindViewHolder(final CalendarRecyclerViewAdapter.DayViewHolder holder, int position) {
            int[] date = dates[position];
            calendar.set(date[0], date[1], date[2]);
            if(date[2] == currentDate[2] && date[1] == currentDate[1] && date[0] == currentDate[0]){
                holder.dayOfMonthTextView.setTextColor(activeColor);
                holder.dayOfWeekTextView.setTextColor(activeColor);
            } else {
                holder.dayOfMonthTextView.setTextColor(inactiveColor);
                holder.dayOfWeekTextView.setTextColor(inactiveColor);
            }
            holder.dayOfMonthTextView.setText(String.valueOf(dates[position][2]));
            holder.dayOfWeekTextView.setText(String.format("%1$ta", calendar));
            if(allEvents[position] == null){
                helper.getEvents(calendar.getTimeInMillis(), new DatabaseHelper.OnEventsLoadListener() {
                    @Override
                    public void onEventsLoaded(ArrayList<Event> events) {
                        Log.d(TAG, "Loaded " + (events!=null));
                        allEvents[holder.getAdapterPosition()] = events == null? new ArrayList() : events;
                        notifyItemChanged(holder.getAdapterPosition());
                    }
                });
            } else {
                ArrayList<Event> events = allEvents[position];
                if(events.size()>0){
                    for(final Event e: events){
                        CardView card = (CardView) LayoutInflater.from(context).inflate(R.layout.event_item, holder.eventContainer, false);
                        TextView title = (TextView) card.findViewById(R.id.titleTextView);
                        TextView location = (TextView) card.findViewById(R.id.locationTextView);
                        TextView time = (TextView) card.findViewById(R.id.timeTextView);
                        time.setVisibility(e.isAllDay()? View.GONE : View.VISIBLE);

                        if(!e.isAllDay()){
                            Calendar tempCalendar = Calendar.getInstance();
                            tempCalendar.setTimeInMillis(e.getStart());
                            StringBuilder builder = new StringBuilder(String.format("%1$tH:%1$tM", tempCalendar));
                            tempCalendar.setTimeInMillis(e.getEnd());
                            builder.append(" - ").append(String.format("%1$tH:%1$tM", tempCalendar));
                            time.setText(builder.toString());
                        }

                        card.setCardBackgroundColor(Color.parseColor(Utils.getColor(e.getColor()).getValue()));
                        title.setText(e.getTitle());
                        location.setText(e.getLocation());

                        card.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                if(listener != null)
                                    listener.onEventClicked(e.getId());
                            }
                        });

                        holder.eventContainer.addView(card);
                    }
                }
            }

        }

        @Override
        public int getItemCount() {
            return dates.length;
        }

        class DayViewHolder extends RecyclerView.ViewHolder{
            final TextView dayOfMonthTextView;
            final TextView dayOfWeekTextView;
            final LinearLayout eventContainer;

            DayViewHolder(View itemView) {
                super(itemView);

                dayOfMonthTextView = (TextView) itemView.findViewById(R.id.dayOfMonthTextView);
                dayOfWeekTextView = (TextView) itemView.findViewById(R.id.dayOfWeekTextView);
                eventContainer = (LinearLayout) itemView.findViewById(R.id.eventContainer);
            }
        }
    }
}
