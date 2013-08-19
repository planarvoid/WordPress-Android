package aspect;


aspect Trace {
    asdasdasdasdas
	pointcut tolog1() : execution(* Activity+.*(..)) ;
    before() : tolog1() {
        String method = thisJoinPoint.getSignature().toShortString();

        Log.d("ASPECTJ", "=========== entering " + method+", parms="+Arrays.toString(thisJoinPoint.getArgs()));
    }
}
