    package com.mot.exception;
    import org.springframework.http.HttpStatus;


    public class UnprocessableEntityException extends BusinessException {
        public UnprocessableEntityException(String mess){
            super(mess);
        }
        @Override
        public HttpStatus getHttpStatus(){
            return HttpStatus.UNPROCESSABLE_ENTITY ;
        }


    }
