package co.ledger.wallet.core.device.usb;
/*
*******************************************************************************
*   BTChip Bitcoin Hardware Wallet Java API
*   (c) 2014 BTChip - 1BTChip7VfTnrPra5jqci7ejnMguuHogTn
*
*  Licensed under the Apache License, Version 2.0 (the "License");
*  you may not use this file except in compliance with the License.
*  You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
*   Unless required by applicable law or agreed to in writing, software
*   distributed under the License is distributed on an "AS IS" BASIS,
*   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
*  See the License for the specific language governing permissions and
*   limitations under the License.
********************************************************************************
*/

import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbRequest;
import android.util.Log;

import org.spongycastle.util.encoders.HexTranslator;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

import co.ledger.wallet.core.utils.HexUtils;
import co.ledger.wallet.core.utils.HexUtils$;
import co.ledger.wallet.core.utils.logs.Logger;
import co.ledger.wallet.core.utils.logs.Logger$;


public class BTChipTransportAndroidHID {

    private UsbDeviceConnection connection;
    private UsbInterface dongleInterface;
    private UsbEndpoint in;
    private UsbEndpoint out;
    private int timeout;
    private byte transferBuffer[];
    private boolean ledger;

    public BTChipTransportAndroidHID(UsbDeviceConnection connection, UsbInterface dongleInterface, UsbEndpoint in, UsbEndpoint out, int timeout, boolean ledger) {
        this.connection = connection;
        this.dongleInterface = dongleInterface;
        this.in = in;
        this.out = out;
        this.ledger = ledger;
        // Compatibility with old prototypes, to be removed
        if (!this.ledger) {
            this.ledger = (in.getEndpointNumber() != out.getEndpointNumber());
        }
        this.timeout = timeout;
        transferBuffer = new byte[HID_BUFFER_SIZE];
    }

    public byte[] exchange(byte[] command) throws Exception {
        ByteArrayOutputStream response = new ByteArrayOutputStream();
        byte[] responseData = null;
        int offset = 0;
        int responseSize;
        int result;

        if (ledger) {
            command = LedgerTransportHelper.wrapCommandAPDU(LEDGER_DEFAULT_CHANNEL, command, HID_BUFFER_SIZE);
        }
        UsbRequest request = new UsbRequest();
        request.initialize(connection, out);
        while(offset != command.length) {
            int blockSize = (command.length - offset > HID_BUFFER_SIZE ? HID_BUFFER_SIZE : command.length - offset);
            System.arraycopy(command, offset, transferBuffer, 0, blockSize);
            request.queue(ByteBuffer.wrap(transferBuffer), HID_BUFFER_SIZE);
            connection.requestWait();
            offset += blockSize;
        }
        ByteBuffer responseBuffer = ByteBuffer.allocate(HID_BUFFER_SIZE);
        request = new UsbRequest();
        request.initialize(connection, in);
        if (!ledger) {
            request.queue(responseBuffer, HID_BUFFER_SIZE);
            connection.requestWait();
            responseBuffer.rewind();
            int sw1 = (int)(responseBuffer.get() & 0xff);
            int sw2 = (int)(responseBuffer.get() & 0xff);
            if (sw1 != SW1_DATA_AVAILABLE) {
                response.write(sw1);
                response.write(sw2);
            }
            else {
                responseSize = sw2 + 2;
                offset = 0;
                int blockSize = (responseSize > HID_BUFFER_SIZE - 2 ? HID_BUFFER_SIZE - 2 : responseSize);
                responseBuffer.get(transferBuffer, 0, blockSize);
                response.write(transferBuffer, 0, blockSize);
                offset += blockSize;
                while (offset != responseSize) {
                    responseBuffer.clear();
                    request.queue(responseBuffer, HID_BUFFER_SIZE);
                    connection.requestWait();
                    responseBuffer.rewind();
                    blockSize = (responseSize - offset > HID_BUFFER_SIZE ? HID_BUFFER_SIZE : responseSize - offset);
                    responseBuffer.get(transferBuffer, 0, blockSize);
                    response.write(transferBuffer, 0, blockSize);
                    offset += blockSize;
                }
                responseBuffer.clear();
            }
            responseData = response.toByteArray();
        }
        else {
            while ((responseData = LedgerTransportHelper.unwrapResponseAPDU(LEDGER_DEFAULT_CHANNEL, response
                    .toByteArray(), HID_BUFFER_SIZE)) == null) {
                responseBuffer.clear();
                request.queue(responseBuffer, HID_BUFFER_SIZE);
                connection.requestWait();
                responseBuffer.rewind();
                responseBuffer.get(transferBuffer, 0, HID_BUFFER_SIZE);
                response.write(transferBuffer, 0, HID_BUFFER_SIZE);
            }
        }
        return responseData;
    }

    public void close() throws Exception {
        connection.releaseInterface(dongleInterface);
        connection.close();
    }

    private static final int HID_BUFFER_SIZE = 64;
    private static final int LEDGER_DEFAULT_CHANNEL = 1;
    private static final int SW1_DATA_AVAILABLE = 0x61;
}