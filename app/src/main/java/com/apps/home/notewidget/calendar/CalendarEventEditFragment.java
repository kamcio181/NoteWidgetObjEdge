package com.apps.home.notewidget.calendar;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SwitchCompat;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CompoundButton;
import android.widget.DatePicker;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.TimePicker;

import com.apps.home.notewidget.R;
import com.apps.home.notewidget.objects.Event;
import com.apps.home.notewidget.utils.DatabaseHelper;
import com.apps.home.notewidget.utils.TitleChangeListener;
import com.apps.home.notewidget.utils.Utils;

import java.util.Calendar;
import java.util.Locale;

/**
 * A placeholder fragment containing a simple view.
 */
public class CalendarEventEditFragment extends Fragment implements TitleChangeListener, CompoundButton.OnCheckedChangeListener, View.OnClickListener{
    private static final String TAG = "CalendarEditItem";
    public static final String ARG_PARAM1 = "param1";
    private Context context;
    private DatabaseHelper helper;
    private ActionBar actionBar;
    private SwitchCompat allDay;
    private TextView startDate, startTime, endDate, endTime, repeat, location, notification, color;
    private DatePickerDialog datePickerDialog;
    private TimePickerDialog timePickerDialog;
    private ImageView colorIV;
    private Calendar startCalendar, endCalendar;
    private boolean isDateCorrect = true;
    private int currentColor = 4;
    private int currentNotification = 30;
    private boolean skipSaving = false;
    private Event event;

    public CalendarEventEditFragment() {
    }

    public static CalendarEventEditFragment newInstance(long id) {
        CalendarEventEditFragment fragment = new CalendarEventEditFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_PARAM1, id);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            event = new Event(getArguments().getLong(ARG_PARAM1));
        }
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_calendar_edit, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        this.context = getActivity();
        helper = new DatabaseHelper(context);
        actionBar = ((AppCompatActivity)context).getSupportActionBar();
        setHasOptionsMenu(true);
        setUpViews(view);

        if(event != null){
            loadEvent();
        } else {
            setUpNewEvent();
        }



    }

    @Override
    public void onStop() {
        super.onStop();
        Log.d(TAG, "Stop, skip saving" + skipSaving);
        String eventLocation = location.getText().toString().trim();
        event.setLocation(eventLocation.length() == 0? "Location unknown" : eventLocation);
        if(!skipSaving){
            saveNote(false);
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
        switch (compoundButton.getId()){
            case R.id.allDaySwitch:
                startTime.setVisibility(b? View.GONE : View.VISIBLE);
                endTime.setVisibility(b? View.GONE : View.VISIBLE);
                Log.d(TAG, "allDaySwitch");
                if(checkDateCorrection())
                    event.setAllDay(b);
                break;
        }
    }

    @Override
    public void onClick(final View view) {
        switch (view.getId()){
            case R.id.startDate:
                datePickerDialog = new DatePickerDialog(getActivity(), new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker datePicker, int i, int i1, int i2) {
                        if (datePicker.isShown()) {
                            startCalendar.set(i, i1, i2);
                            startDate.setText(String.format(Locale.getDefault(), "%1$ta, %1$tb %1$te, %1$tY", startCalendar));
                            Log.d(TAG, "startDate");
                            if(checkDateCorrection())
                                event.setStart(startCalendar.getTimeInMillis());
                        }
                    }
                }, startCalendar.get(Calendar.YEAR), startCalendar.get(Calendar.MONTH), startCalendar.get(Calendar.DAY_OF_MONTH));
                datePickerDialog.show();
                break;
            case R.id.endDate:
                datePickerDialog = new DatePickerDialog(getActivity(), new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker datePicker, int i, int i1, int i2) {
                        if (datePicker.isShown()) {
                            endCalendar.set(i, i1, i2);
                            endDate.setText(String.format(Locale.getDefault(), "%1$ta, %1$tb %1$te, %1$tY", endCalendar));
                            Log.d(TAG, "endDate");
                            if(checkDateCorrection())
                                event.setEnd(endCalendar.getTimeInMillis());
                        }
                    }
                }, endCalendar.get(Calendar.YEAR), endCalendar.get(Calendar.MONTH), endCalendar.get(Calendar.DAY_OF_MONTH));
                datePickerDialog.show();
                break;
            case R.id.startTime:
                timePickerDialog = new TimePickerDialog(getActivity(), new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker timePicker, int i, int i1) {
                        if (timePicker.isShown()) {
                            startCalendar.set(Calendar.HOUR_OF_DAY, i);
                            startCalendar.set(Calendar.MINUTE, i1);
                            startTime.setText(String.format(Locale.getDefault(), "%1$tH:%1$tM", startCalendar));
                            Log.d(TAG, "startTime");
                            if(checkDateCorrection())
                                event.setStart(startCalendar.getTimeInMillis());
                        }
                    }
                }, startCalendar.get(Calendar.HOUR_OF_DAY), startCalendar.get(Calendar.MINUTE), true);
                timePickerDialog.show();
                break;
            case R.id.endTime:
                timePickerDialog = new TimePickerDialog(getActivity(), new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker timePicker, int i, int i1) {
                        if (timePicker.isShown()) {
                            endCalendar.set(Calendar.HOUR_OF_DAY, i);
                            endCalendar.set(Calendar.MINUTE, i1);
                            endTime.setText(String.format(Locale.getDefault(), "%1$tH:%1$tM", endCalendar));
                            Log.d(TAG, "endTime");
                            if(checkDateCorrection())
                                event.setEnd(endCalendar.getTimeInMillis());
                        }
                    }
                }, endCalendar.get(Calendar.HOUR_OF_DAY), endCalendar.get(Calendar.MINUTE), true);
                timePickerDialog.show();
                break;
            case R.id.color:
                getColorDialog();
                break;
            case R.id.notify:
                getNotificationDialog();
                break;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.v(TAG, "onOptionsItemSelected");

        switch (item.getItemId()){
            case R.id.action_delete:
                deleteNote();
                break;
            case R.id.action_discard_changes:
                discardChanges();
                break;
            case R.id.action_save:
                saveNote(true);
                break;
        }
        return true;
    }

    private void setUpViews(View view){
        allDay = (SwitchCompat) view.findViewById(R.id.allDaySwitch);
        startDate = (TextView) view.findViewById(R.id.startDate);
        startTime = (TextView) view.findViewById(R.id.startTime);
        endDate = (TextView) view.findViewById(R.id.endDate);
        endTime = (TextView) view.findViewById(R.id.endTime);
        repeat = (TextView) view.findViewById(R.id.repeat);
        location = (TextView) view.findViewById(R.id.location);
        notification = (TextView) view.findViewById(R.id.notify);
        color = (TextView) view.findViewById(R.id.color);
        colorIV = (ImageView) view.findViewById(R.id.colorIV);
        allDay.setOnCheckedChangeListener(this);
        startDate.setOnClickListener(this);
        startTime.setOnClickListener(this);
        endDate.setOnClickListener(this);
        endTime.setOnClickListener(this);
        repeat.setOnClickListener(this);
        color.setOnClickListener(this);
        notification.setOnClickListener(this);
    }

    private void loadEvent(){
        //TODO remember to calculate from minutes to milliseconds
        helper.getEvent(event.getId(), new DatabaseHelper.OnEventLoadListener() {
            @Override
            public void onEventLoaded(Event event) {
                CalendarEventEditFragment.this.event = event;
                startCalendar = Calendar.getInstance();
                startCalendar.setTimeInMillis(event.getStart());
                endCalendar = Calendar.getInstance();
                endCalendar.setTimeInMillis(event.getEnd());
                currentColor = event.getColor();
                setValues();
            }
        });

    }

    private void setUpNewEvent(){
        startCalendar = Calendar.getInstance();
        startCalendar.set(Calendar.SECOND, 0);
        startCalendar.set(Calendar.MILLISECOND, 0);
        endCalendar = Calendar.getInstance();
        endCalendar.setTimeInMillis(endCalendar.getTimeInMillis()+1000*60*60);

        event = new Event(-1, "New Event", false, startCalendar.getTimeInMillis(),
                endCalendar.getTimeInMillis(), "Unknown location", currentNotification, currentColor);

        setValues();
    }

    private void setValues(){
        actionBar.setTitle(event.getTitle());
        allDay.setChecked(event.isAllDay());
        startDate.setText(String.format(Locale.getDefault(), "%1$ta, %1$tb %1$te, %1$tY", startCalendar));
        endDate.setText(String.format(Locale.getDefault(), "%1$ta, %1$tb %1$te, %1$tY", endCalendar));
        startTime.setText(String.format(Locale.getDefault(), "%1$tH:%1$tM", startCalendar));
        endTime.setText(String.format(Locale.getDefault(), "%1$tH:%1$tM", endCalendar));
        location.setText(event.getLocation());
        notification.setText(Utils.getNotificationTypes().get(event.getNotification()));
        setColor();
    }

    private boolean checkDateCorrection(){
        int divider = allDay.isChecked()? (1000*60*60*24) : (1000*60); //compare days or minutes
        if((startCalendar.getTimeInMillis()/divider) > (endCalendar.getTimeInMillis()/divider) && isDateCorrect){
            Log.d(TAG, "change " + false);
            isDateCorrect = false;
            setStartDateColor();
        } else if((startCalendar.getTimeInMillis()/divider) <= (endCalendar.getTimeInMillis()/divider) && !isDateCorrect){
            Log.d(TAG, "change " + true);
            isDateCorrect = true;
            setStartDateColor();
        }
        return isDateCorrect;
    }

    private void setStartDateColor() {
        if (isDateCorrect) {
            startDate.setTextColor(Color.BLACK);
            startTime.setTextColor(Color.BLACK);
        } else {
            startDate.setTextColor(Color.RED);
            startTime.setTextColor(Color.RED);
        }
    }

    private void getNotificationDialog(){
        final SimpleAdapterWithDefault adapter = new SimpleAdapterWithDefault(Utils.getNotificationTypes(), currentNotification);
        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setAdapter(adapter, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                currentNotification = (int) adapter.getItemId(i);
                notification.setText((CharSequence) adapter.getItem(i));
                event.setNotification(currentNotification);
            }
        });
        builder.create().show();
    }

    private void getColorDialog(){
        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setAdapter(new ColorAdapter(), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                currentColor = i;
                setColor();
            }
        });
        builder.create().show();
    }

    private void setColor(){
        int colorValue = Color.parseColor(Utils.getColor(currentColor).getValue());
        colorIV.setColorFilter(colorValue);
        color.setText(Utils.getColor(currentColor).getName());
        event.setColor(currentColor);
        actionBar.setBackgroundDrawable(new ColorDrawable(colorValue));
    }

    private void discardChanges(){
        Utils.showToast(context, context.getString(R.string.closed_without_saving));
        skipSaving = true;
        ((AppCompatActivity)context).onBackPressed();
    }

    private void deleteNote() {
        Utils.showToast(context, "Deleting");
        skipSaving = true;
        helper.removeEvent(event.getId(), new DatabaseHelper.OnItemRemoveListener() {
            @Override
            public void onItemRemoved(int numberOfRows) {
                ((AppCompatActivity)context).onBackPressed();
            }
        });
    }

    private void saveNote(final boolean quitAfterSaving) {
        Utils.showToast(context.getApplicationContext(), getString(R.string.saving));
        Log.d(TAG, "Event to save " + event.toString());
        if(event.getId() == -1) {
            helper.createEvent(event, new DatabaseHelper.OnItemInsertListener() {
                @Override
                public void onItemInserted(long id) {
                    Log.d(TAG, "event saved " + id);
                    event.setId(id);

                    if(quitAfterSaving)
                        ((AppCompatActivity)context).onBackPressed();
                }
            });
        } else{
            helper.updateEvent(event, new DatabaseHelper.OnItemUpdateListener() {
                @Override
                public void onItemUpdated(int numberOfRows) {
                    Log.d(TAG, "event updated");
                    if(quitAfterSaving)
                        ((AppCompatActivity)context).onBackPressed();
                }
            });
        }
    }

    @Override
    public void onTitleChanged(String newTitle) {
        event.setTitle(newTitle);
    }

    private class ColorAdapter extends BaseAdapter{
        private com.apps.home.notewidget.objects.Color[] colors = Utils.getColors();

        @Override
        public int getCount() {
            return colors.length;
        }

        @Override
        public Object getItem(int i) {
            return colors[i];
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            SingleRowWithColorViewHolder holder;

            if(view == null) {
                LayoutInflater layoutInflater = LayoutInflater.from(getActivity());
                view = layoutInflater.inflate(R.layout.single_line_with_color_item_default, viewGroup, false);
                holder = new SingleRowWithColorViewHolder();
                holder.imageView = (ImageView) view.findViewById(R.id.colorIV);
                holder.textView = (TextView) view.findViewById(R.id.textView2);
                holder.defaultIV = (ImageView) view.findViewById(R.id.defaultItem);
                view.setTag(holder);
            } else {
                holder = (SingleRowWithColorViewHolder) view.getTag();
            }

            holder.imageView.setColorFilter(Color.parseColor(colors[i].getValue()));
            holder.textView.setText(colors[i].getName());
            if(i == currentColor) {
                holder.defaultIV.setVisibility(View.VISIBLE);
                holder.textView.setTextColor(ContextCompat.getColor(getActivity(), R.color.colorPrimary));
            } else {
                holder.defaultIV.setVisibility(View.GONE);
                holder.textView.setTextColor(Color.BLACK);
            }
            return view;
        }

        private class SingleRowWithColorViewHolder{
            ImageView imageView, defaultIV;
            TextView textView;
        }
    }

    private class SimpleAdapterWithDefault extends BaseAdapter{
        private SparseArray<String> items;
        private int currentDefault;

        public SimpleAdapterWithDefault(SparseArray<String> items, int currentDefault) {
            this.items = items;
            this.currentDefault = currentDefault;
        }

        @Override
        public int getCount() {
            return items.size();
        }

        @Override
        public Object getItem(int i) {
            return items.valueAt(i);
        }

        @Override
        public long getItemId(int i) {
            return items.keyAt(i);
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            SingleRowWithColorViewHolder holder;

            if(view == null) {
                LayoutInflater layoutInflater = LayoutInflater.from(getActivity());
                view = layoutInflater.inflate(R.layout.single_line_item_default, viewGroup, false);
                holder = new SingleRowWithColorViewHolder();
                holder.textView = (TextView) view.findViewById(R.id.textView2);
                holder.defaultIV = (ImageView) view.findViewById(R.id.defaultItem);
                view.setTag(holder);
            } else {
                holder = (SingleRowWithColorViewHolder) view.getTag();
            }

            holder.textView.setText(items.valueAt(i));
            if(items.keyAt(i) == currentDefault) {
                holder.defaultIV.setVisibility(View.VISIBLE);
                holder.textView.setTextColor(ContextCompat.getColor(getActivity(), R.color.colorPrimary));
            } else {
                holder.defaultIV.setVisibility(View.GONE);
                holder.textView.setTextColor(Color.BLACK);
            }
            return view;
        }

        private class SingleRowWithColorViewHolder{
            ImageView defaultIV;
            TextView textView;
        }
    }
}
