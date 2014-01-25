
package com.kb.gamepad;

import com.kb.gamepad.R;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.res.Resources;
import android.hardware.input.InputManager;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.InputDevice.MotionRange;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class GamepadDemo extends Activity
        implements InputManager.InputDeviceListener {
    private static final String TAG = "GamepadDemo";

    private InputManager mInputManager;
    private SparseArray<InputDeviceState> mInputDeviceStates;
    //private GameView mGame;
    private ListView mSummaryList;
    private SummaryAdapter mSummaryAdapter;

    static void log(String s) { Log.e(TAG, s); }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        log("onCreate");
        super.onCreate(savedInstanceState);

        mInputManager = (InputManager)getSystemService(Context.INPUT_SERVICE);

        mInputDeviceStates = new SparseArray<InputDeviceState>();
        mSummaryAdapter = new SummaryAdapter(this, getResources());

        setContentView(R.layout.main);
        
        // Per Stackoverflow advice.
        View mainView = findViewById(R.id.content);
        mainView.setFocusable(true);
        mainView.setFocusableInTouchMode(true);
        boolean takeFocus = mainView.requestFocus();
        log((takeFocus ? "got" : "cannot get") + " focus");

        //mGame = (GameView) findViewById(R.id.game);

        mSummaryList = (ListView) findViewById(R.id.summary);
        mSummaryList.setAdapter(mSummaryAdapter);
        mSummaryList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                mSummaryAdapter.onItemClick(position);
            }
        });
    }

    @Override
    protected void onResume() {
        log("onResume");
        super.onResume();

        // Register an input device listener to watch when input devices are
        // added, removed or reconfigured.
        mInputManager.registerInputDeviceListener(this, null);

        // Query all input devices.
        // We do this so that we can see them in the log as they are enumerated.
        int[] ids = mInputManager.getInputDeviceIds();
        for (int i = 0; i < ids.length; i++) {
            getInputDeviceState(ids[i]);
        }
    }

    @Override
    protected void onPause() {
        log("onPause");
        super.onPause();

        // Remove the input device listener when the activity is paused.
        mInputManager.unregisterInputDeviceListener(this);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
    	log("KeyEvent: code=" + Integer.toString(event.getKeyCode()));
        // Update device state for visualization and logging.
        InputDeviceState state = getInputDeviceState(event.getDeviceId());
        if (state != null) {
            switch (event.getAction()) {
                case KeyEvent.ACTION_DOWN:
                    if (state.onKeyDown(event)) {
                        mSummaryAdapter.show(state);
                    }
                    return true;
                case KeyEvent.ACTION_UP:
                    if (state.onKeyUp(event)) {
                        mSummaryAdapter.show(state);
                    }
                    return true;
            }
        }
        return super.dispatchKeyEvent(event);
    }
    
    @Override
    public boolean dispatchGenericMotionEvent(MotionEvent event) {
        if (isJoystick(event.getSource())
                && event.getAction() == MotionEvent.ACTION_MOVE) {
            // Update device state for visualization and logging.
            InputDeviceState state = getInputDeviceState(event.getDeviceId());
            if (state != null && state.onJoystickMotion(event)) {
                mSummaryAdapter.show(state);
            }
        }
        return super.dispatchGenericMotionEvent(event);
    }
   
    private InputDeviceState getInputDeviceState(int deviceId) {
        InputDeviceState state = mInputDeviceStates.get(deviceId);
        if (state == null) {
            final InputDevice device = mInputManager.getInputDevice(deviceId);
            if (device == null) {
                return null;
            }
            state = new InputDeviceState(device);
            mInputDeviceStates.put(deviceId, state);
            mSummaryAdapter.show(state);
            Log.i(TAG, "Device enumerated: " + state.mDevice);
        }
        return state;
    }

    // Implementation of InputManager.InputDeviceListener.onInputDeviceAdded()
    @Override
    public void onInputDeviceAdded(int deviceId) {
        InputDeviceState state = getInputDeviceState(deviceId);
        Log.i(TAG, "Device added: " + state.mDevice);
    }

    // Implementation of InputManager.InputDeviceListener.onInputDeviceChanged()
    @Override
    public void onInputDeviceChanged(int deviceId) {
        InputDeviceState state = mInputDeviceStates.get(deviceId);
        if (state != null) {
            mInputDeviceStates.remove(deviceId);
            state = getInputDeviceState(deviceId);
            Log.i(TAG, "Device changed: " + state.mDevice);
        }
    }

    // Implementation of InputManager.InputDeviceListener.onInputDeviceRemoved()
    @Override
    public void onInputDeviceRemoved(int deviceId) {
        InputDeviceState state = mInputDeviceStates.get(deviceId);
        if (state != null) {
            Log.i(TAG, "Device removed: " + state.mDevice);
            mInputDeviceStates.remove(deviceId);
        }
    }

    private static boolean isJoystick(int source) {
        return (source & InputDevice.SOURCE_CLASS_JOYSTICK) != 0;
    }

    private static class InputDeviceState {
        private final InputDevice mDevice;
        private final int[] mAxes;
        private final float[] mAxisValues;
        private final SparseIntArray mKeys;
        private final float[] wkaxes;
        private float[] wkbuttons;
        
        public InputDeviceState(InputDevice device) {
            mDevice = device;

            int numAxes = 0;
            final List<MotionRange> ranges = device.getMotionRanges();
            for (MotionRange range : ranges) {
                if ((range.getSource() & InputDevice.SOURCE_CLASS_JOYSTICK) != 0) {
                    numAxes += 1;
                }
            }

            mAxes = new int[numAxes];
            mAxisValues = new float[numAxes];
            wkaxes = new float[numAxes];
            int i = 0;
            for (MotionRange range : ranges) {
                if ((range.getSource() & InputDevice.SOURCE_CLASS_JOYSTICK) != 0) {
                    mAxes[i++] = range.getAxis();
                }
            }

            mKeys = new SparseIntArray();
        }

        public InputDevice getDevice() {
            return mDevice;
        }

        public int getAxisCount() {
            return mAxes.length;
        }

        public int getAxis(int axisIndex) {
            return mAxes[axisIndex];
        }

        public float getAxisValue(int axisIndex) {
            return mAxisValues[axisIndex];
        }

        public int getKeyCount() {
            return mKeys.size();
        }

        public int getKeyCode(int keyIndex) {
            return mKeys.keyAt(keyIndex);
        }

        public boolean isKeyPressed(int keyIndex) {
            return mKeys.valueAt(keyIndex) != 0;
        }

        public boolean onKeyDown(KeyEvent event) {
            final int keyCode = event.getKeyCode();
            if (isGameKey(keyCode)) {
                if (event.getRepeatCount() == 0) {
                    final String symbolicName = KeyEvent.keyCodeToString(keyCode);
                    mKeys.put(keyCode, 1);
                    Log.i(TAG, mDevice.getName() + " - Key Down: " + symbolicName);
                }
                return true;
            }
            return false;
        }

        public boolean onKeyUp(KeyEvent event) {
            final int keyCode = event.getKeyCode();
            if (isGameKey(keyCode)) {
                int index = mKeys.indexOfKey(keyCode);
                if (index >= 0) {
                    final String symbolicName = KeyEvent.keyCodeToString(keyCode);
                    mKeys.put(keyCode, 0);
                    Log.i(TAG, mDevice.getName() + " - Key Up: " + symbolicName);
                }
                return true;
            }
            return false;
        }

        public boolean onJoystickMotion(MotionEvent event) {
            StringBuilder message = new StringBuilder();
            message.append(mDevice.getName()).append(" - Joystick Motion:\n");

            final int historySize = event.getHistorySize();
            for (int i = 0; i < mAxes.length; i++) {
                final int axis = mAxes[i];
                final float value = event.getAxisValue(axis);
                mAxisValues[i] = value;
                message.append("  ").append(MotionEvent.axisToString(axis)).append(": ");

                // Append all historical values in the batch.
                for (int historyPos = 0; historyPos < historySize; historyPos++) {
                    message.append(event.getHistoricalAxisValue(axis, historyPos));
                    message.append(", ");
                }

                // Append the current value.
                message.append(value);
                message.append("\n");
            }
            Log.i(TAG, message.toString());
            return true;
        }

        // Check whether this is a key we care about.
        // In a real game, we would probably let the user configure which keys to use
        // instead of hardcoding the keys like this.
        private static boolean isGameKey(int keyCode) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_DPAD_UP:
                case KeyEvent.KEYCODE_DPAD_DOWN:
                case KeyEvent.KEYCODE_DPAD_LEFT:
                case KeyEvent.KEYCODE_DPAD_RIGHT:
                case KeyEvent.KEYCODE_DPAD_CENTER:
                case KeyEvent.KEYCODE_SPACE:
                    return true;
                default:
                    return KeyEvent.isGamepadButton(keyCode);
            }
        }
        
        public float[] getWKAxes() { return wkaxes; }
        public float[] getWKButtons() { return wkbuttons; }
        
        private void generateWK() {
        	
        }
    }

    private static class SummaryAdapter extends BaseAdapter {
        private static final int BASE_ID_HEADING = 1 << 10;
        private static final int BASE_ID_DEVICE_ITEM = 2 << 10;
        private static final int BASE_ID_AXIS_ITEM = 3 << 10;
        private static final int BASE_ID_KEY_ITEM = 4 << 10;

        private final Context mContext;
        //private final Resources mResources;

        private final SparseArray<Item> mDataItems = new SparseArray<Item>();
        private final ArrayList<Item> mVisibleItems = new ArrayList<Item>();

        private final Item mDeviceNameTextColumn;

        private final Item mAxesHeading;
        private final Item mKeysHeading;

        private InputDeviceState mState;

        public SummaryAdapter(Context context, Resources resources) {
            mContext = context;
            //mResources = resources;
            mDeviceNameTextColumn = new Item(mContext, BASE_ID_DEVICE_ITEM);
            mAxesHeading = new Item(mContext, BASE_ID_HEADING + 1);
            mAxesHeading.setContent("--axes--");
            mKeysHeading = new Item(mContext, BASE_ID_HEADING + 2);
            mKeysHeading.setContent("--keys--");
        }

        public void onItemClick(int position) {
            if (mState != null) {
                Toast toast = Toast.makeText(
                        mContext, mState.getDevice().toString(), Toast.LENGTH_LONG);
                toast.show();
            }
        }

        public void show(InputDeviceState state) {
            mState = state;
            mVisibleItems.clear();

            // Populate device information.
            mDeviceNameTextColumn.setContent("device:" + state.getDevice().getName());
            mVisibleItems.add(mDeviceNameTextColumn);

            // Populate axes.
            mVisibleItems.add(mAxesHeading);
            final int axisCount = state.getAxisCount();
            for (int i = 0; i < axisCount; i++) {
                final int axis = state.getAxis(i);
                final int id = BASE_ID_AXIS_ITEM | axis;
                Item column = (Item) mDataItems.get(id);
                if (column == null) {
                    column = new Item(mContext, id);
                    mDataItems.put(id, column);
                }
                //column.setContent(Integer.valueOf(axis).toString() + " -> " +  Float.toString(state.getAxisValue(i)));
                column.setContent(MotionEvent.axisToString(axis) + "(" + Integer.toString(axis) + ")" + 
                		" -> " +  Float.toString(state.getAxisValue(i)));
                mVisibleItems.add(column);
            }

            // Populate keys.
            mVisibleItems.add(mKeysHeading);
            final int keyCount = state.getKeyCount();
            for (int i = 0; i < keyCount; i++) {
                final int keyCode = state.getKeyCode(i);
                final int id = BASE_ID_KEY_ITEM | keyCode;
                Item column = (Item) mDataItems.get(id);
                if (column == null) {
                    column = new Item(mContext, id);
                    mDataItems.put(id, column);
                }
                column.setContent(KeyEvent.keyCodeToString(keyCode) + "(" + keyCode + ")" +  
                		" -> " + (state.isKeyPressed(i) ? "pressed" : "released"));
                mVisibleItems.add(column);
            }

            notifyDataSetChanged();
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

        @Override
        public int getCount() {
            return mVisibleItems.size();
        }

        @Override
        public Item getItem(int position) {
            return mVisibleItems.get(position);
        }

        @Override
        public long getItemId(int position) {
            return getItem(position).getItemId();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            return getItem(position).getView(convertView, parent);
        }

        private static class Item {
            private final int mItemId;
            private TextView mView;

            public Item(Context ctx, int itemId) {
                mItemId = itemId;
                mView = new TextView(ctx);
            }

            public long getItemId() {
                return mItemId;
            }

            public void setContent(String s) {
                mView.setText(s);
            }

            public View getView(View convertView, ViewGroup parent) {
                return mView;
            }
        }
    }
}
