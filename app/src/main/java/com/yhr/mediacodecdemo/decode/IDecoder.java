package com.yhr.mediacodecdemo.decode;

public interface IDecoder extends Runnable {

    void start();

    void pause();

    void stop();

}
