package com.mikescher.xamarinforms.resourcebuilder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class ProcessHelper {

	private static class StreamGobbler extends Thread
	{
		private StringBuilder result;

		private InputStream is;

		public StreamGobbler(InputStream is)
		{
			this.is = is;
			this.result = new StringBuilder();
		}

		@Override
		public void run()
		{
			try
			{
				InputStreamReader isr = new InputStreamReader(is);
				BufferedReader br = new BufferedReader(isr);

				char[] bfr = new char[128];
				for(;;)
				{
					int bfr_len = br.read(bfr);
					if (bfr_len < 0) break;
					if (bfr_len == 0) continue;

					result.append(bfr, 0, bfr_len);
				}
			} catch (Exception ioe)
			{
				System.err.println(ioe);
			}
		}

		public String get() {
			if (isAlive()) return "";
			return result.toString();
		}

		public void waitFor() {
			while (this.isAlive()) ThreadUtils.safeSleep(1);
		}
	}

	public static Tuple3<Integer, String, String> procExec(String cmd, String... args) throws IOException
	{
		Runtime rt = Runtime.getRuntime();
		String[] commands = new String[args.length+1];
		commands[0] = cmd;
		System.arraycopy(args, 0, commands, 1, args.length);
		Process proc = rt.exec(commands);

		StreamGobbler errorGobbler = new StreamGobbler(proc.getErrorStream());
		StreamGobbler outputGobbler = new StreamGobbler(proc.getInputStream());

		errorGobbler.start();
		outputGobbler.start();

		try {
			int exitVal = proc.waitFor();

			errorGobbler.waitFor();
			outputGobbler.waitFor();

			return Tuple3.Create(exitVal, outputGobbler.get(), errorGobbler.get());
		}
		catch (InterruptedException e)
		{
			return Tuple3.Create(proc.exitValue(), outputGobbler.get(), errorGobbler.get());
		}
	}
}
