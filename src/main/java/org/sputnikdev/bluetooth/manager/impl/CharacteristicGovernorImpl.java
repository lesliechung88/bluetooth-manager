package org.sputnikdev.bluetooth.manager.impl;

/*-
 * #%L
 * org.sputnikdev:bluetooth-manager
 * %%
 * Copyright (C) 2017 Sputnik Dev
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sputnikdev.bluetooth.URL;
import org.sputnikdev.bluetooth.manager.BluetoothGovernor;
import org.sputnikdev.bluetooth.manager.BluetoothObjectType;
import org.sputnikdev.bluetooth.manager.BluetoothObjectVisitor;
import org.sputnikdev.bluetooth.manager.CharacteristicGovernor;
import org.sputnikdev.bluetooth.manager.NotReadyException;
import org.sputnikdev.bluetooth.manager.ValueListener;
import tinyb.BluetoothException;

/**
 *
 * @author Vlad Kolotov
 */
class CharacteristicGovernorImpl extends BluetoothObjectGovernor<Characteristic> implements CharacteristicGovernor {

    private Logger logger = LoggerFactory.getLogger(CharacteristicGovernorImpl.class);

    private ValueListener valueListener;
    private ValueNotification valueNotification;

    CharacteristicGovernorImpl(BluetoothManagerImpl bluetoothManager, URL url) {
        super(bluetoothManager, url);
    }

    @Override
    Characteristic findBluetoothObject() {
        return BluetoothObjectFactory.getDefault().getCharacteristic(getURL());
    }

    @Override
    void disableNotifications(Characteristic characteristic) {
        logger.info("Disable characteristic notifications: " + getURL());
        if (characteristic.isNotifying()) {
            characteristic.disableValueNotifications();
            valueNotification = null;
        }
    }

    @Override
    void init(Characteristic characteristic) {
        enableNotification(characteristic);
    }

    @Override
    void dispose() { }

    @Override
    void updateState(Characteristic characteristic) { }

    @Override
    public void addValueListener(ValueListener valueListener) {
        this.valueListener = valueListener;
    }

    @Override
    public void removeValueListener(ValueListener valueListener) {
        this.valueListener = null;
    }

    @Override
    public String[] getFlags() throws NotReadyException {
        return getBluetoothObject().getFlags();
    }

    @Override
    public boolean isNotifiable() throws NotReadyException {
        List<String> flgs = Arrays.asList(getFlags());
        return flgs.contains(NOTIFY_FLAG) || flgs.contains(INDICATE_FLAG);
    }

    @Override
    public boolean isWritable() throws NotReadyException {
        return Arrays.asList(getFlags()).contains(WRITE_FLAG);
    }

    @Override
    public boolean isReadable() throws NotReadyException {
        return Arrays.asList(getFlags()).contains(READ_FLAG);
    }

    @Override
    public byte[] read() throws NotReadyException {
        Characteristic characteristic = getBluetoothObject();
        if (characteristic == null) {
            throw new IllegalStateException("Characteristic governor is not initialized");
        }
        byte[] result = characteristic.readValue();
        updateLastUpdated();
        return result;
    }

    @Override
    public boolean write(byte[] data) throws NotReadyException {
        Characteristic characteristic = getBluetoothObject();
        if (characteristic == null) {
            throw new IllegalStateException("Characteristic governor is not initialized");
        }
        boolean result = characteristic.writeValue(data);
        updateLastUpdated();
        return result;
    }

    @Override
    public String toString() {
        return "[Characteristic] " + getURL();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        BluetoothGovernor that = (BluetoothGovernor) o;
        return url.equals(that.getURL());
    }

    @Override
    public int hashCode() {
        int result = url.hashCode();
        result = 31 * result + url.hashCode();
        return result;
    }

    @Override
    public BluetoothObjectType getType() {
        return BluetoothObjectType.CHARACTERISTIC;
    }

    @Override
    public void accept(BluetoothObjectVisitor visitor) throws Exception {
        visitor.visit(this);
    }

    private void enableNotification(Characteristic characteristic) {
        logger.info("Enable characteristic notifications: " + getURL());
        this.valueNotification = new ValueNotification();
        if (canNotify(characteristic) && !characteristic.isNotifying()) {
            characteristic.enableValueNotifications(valueNotification);
        }
    }

    private static boolean canNotify(Characteristic characteristic) {
        List<String> flgs = Arrays.asList(characteristic.getFlags());
        return flgs.contains(NOTIFY_FLAG) || flgs.contains(INDICATE_FLAG);
    }

    private class ValueNotification implements Notification<byte[]> {
        @Override
        public void notify(byte[] data) {
            try {
                ValueListener listener = valueListener;
                if (listener != null) {
                    listener.changed(data);
                }
            } catch (BluetoothException ex) {
                logger.error("Execution error of a characteristic listener", ex);
            }
        }
    }

}