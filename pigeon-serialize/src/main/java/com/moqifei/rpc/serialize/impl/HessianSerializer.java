package com.moqifei.rpc.serialize.impl;


import com.caucho.hessian.io.Hessian2Input;
import com.caucho.hessian.io.Hessian2Output;
import com.moqifei.rpc.serialize.Serializer;
import com.moqifei.rpc.serialize.exception.SerializeException;
import com.moqifei.rpc.spi.PigeonSPI;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

@PigeonSPI("hessian2")
public class HessianSerializer implements Serializer {

	@Override
	public <T> byte[] serialize(T obj){
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		Hessian2Output ho = new Hessian2Output(os);
		try {
			ho.writeObject(obj);
			ho.flush();
			byte[] result = os.toByteArray();
			return result;
		} catch (IOException e) {
			throw new SerializeException(e);
		} finally {
			try {
				ho.close();
			} catch (IOException e) {
				throw new SerializeException(e);
			}
			try {
				os.close();
			} catch (IOException e) {
				throw new SerializeException(e);
			}
		}

	}

	@Override
	public <T> Object deserialize(byte[] bytes, Class<T> clazz) {
		ByteArrayInputStream is = new ByteArrayInputStream(bytes);
		Hessian2Input hi = new Hessian2Input(is);
		try {
			Object result = hi.readObject();
			return result;
		} catch (IOException e) {
			throw new SerializeException(e);
		} finally {
			try {
				hi.close();
			} catch (Exception e) {
				throw new SerializeException(e);
			}
			try {
				is.close();
			} catch (IOException e) {
				throw new SerializeException(e);
			}
		}
	}
	
}
