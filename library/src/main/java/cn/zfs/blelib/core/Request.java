/******************************************************************************
 *  Copyright (C) Cambridge Silicon Radio Limited 2014
 *
 *  This software is provided to the customer for evaluation
 *  purposes only and, as such early feedback on performance and operation
 *  is anticipated. The software source code is subject to change and
 *  not intended for production. Use of developmental release software is
 *  at the user's own risk. This software is provided "as is," and CSR
 *  cautions users to determine for themselves the suitability of using the
 *  beta release version of this software. CSR makes no warranty or
 *  representation whatsoever of merchantability or fitness of the product
 *  for any particular purpose or use. In no event shall CSR be liable for
 *  any consequential, incidental or special damages whatsoever arising out
 *  of the use of or inability to use this software, even if the user has
 *  advised CSR of the possibility of such damages.
 *
 ******************************************************************************/

package cn.zfs.blelib.core;

import android.support.annotation.NonNull;

import java.util.UUID;

/**
 * 描述: 用作请求队列
 * 时间: 2018/4/11 15:15
 * 作者: zengfansheng
 */
public class Request {
    
    public enum RequestType {
        CHARACTERISTIC_NOTIFICATION, CHARACTERISTIC_INDICATION, READ_CHARACTERISTIC, READ_DESCRIPTOR, READ_RSSI, WRITE_CHARACTERISTIC, WRITE_DESCRIPTOR, SET_MTU
    }

    public RequestType type;
    public UUID service;
    public UUID characteristic;
    public UUID descriptor;
    public String requestId;
    public byte[] value;

    public Request(@NonNull RequestType type, @NonNull String requestId, UUID service, UUID characteristic, UUID descriptor) {
        this.type = type;
        this.requestId = requestId;
        this.service = service;
        this.characteristic = characteristic;
        this.descriptor = descriptor;
        this.value = null;
    }

    public Request(@NonNull RequestType type, @NonNull String requestId, UUID service, UUID characteristic, UUID descriptor, byte[] value) {
        this.type = type;
        this.requestId = requestId;
        this.service = service;
        this.characteristic = characteristic;
        this.descriptor = descriptor;
        this.value = value;
    }
}
