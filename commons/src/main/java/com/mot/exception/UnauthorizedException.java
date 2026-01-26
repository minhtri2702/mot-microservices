package com.mot.exception;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class UnauthorizedException extends BusinessException {
    public UnauthorizedException(){
        super();
    }
    @Override
    public HttpStatus getHttpStatus(){
        return HttpStatus.UNAUTHORIZED ;
    }
    public UnauthorizedException(String message) {
        super(message);
    }

}
