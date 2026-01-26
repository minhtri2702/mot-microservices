package com.mot.exception;

import org.springframework.http.HttpStatus;

public class MasterDataIsNotFound extends  BusinessException {
    @Override
    protected HttpStatus getHttpStatus() {
        return HttpStatus.FOUND;
    }
    public MasterDataIsNotFound(String mess){
        super(mess);
    }


}
