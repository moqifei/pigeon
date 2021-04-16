package com.moqifei.rpc.exchange;

import com.moqifei.rpc.net.ChannelHandler;
import com.moqifei.rpc.net.exception.RemotingException;
import com.moqifei.rpc.net.url.PigeonURL;

public interface Exchanger {
    ExchangeServer bind(PigeonURL var1, ChannelHandler var2) throws RemotingException;

    ExchangeClient connect(PigeonURL var1, ChannelHandler var2) throws RemotingException;
}
