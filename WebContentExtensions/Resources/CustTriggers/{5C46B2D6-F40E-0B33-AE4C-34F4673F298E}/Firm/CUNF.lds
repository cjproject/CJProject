TRIGGER X_TRANSACTION_DEL_$V(firm)
ON DELETE X_$V(firm)_TRANSACTION
VAR
  old_transactionRef 		INTEGER;

BEGIN
 old_transactionRef		:= :OLD.LOGICALREF;
 
 DELETE FROM X_$V(firm)_APPROVAL
 WHERE TRANSREF = old_transactionRef;
 
END;
