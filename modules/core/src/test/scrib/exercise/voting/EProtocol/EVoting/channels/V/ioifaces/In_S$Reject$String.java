package exercise.voting.EProtocol.EVoting.channels.V.ioifaces;

import java.io.IOException;
import exercise.voting.EProtocol.EVoting.*;
import exercise.voting.EProtocol.EVoting.roles.*;
import exercise.voting.EProtocol.EVoting.ops.*;

public interface In_S$Reject$String<__Succ extends Succ_In_S$Reject$String> {

	abstract public exercise.voting.EProtocol.EVoting.channels.V.EndSocket receive(S role, Reject op, org.scribble.net.Buf<? super java.lang.String> arg1) throws org.scribble.main.ScribbleRuntimeException, java.io.IOException, ClassNotFoundException;
}