package ext.linmp;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.scribble.main.ScribbleException;
import org.scribble.util.ScribParserException;

import main.LinMPSyntaxException;
import main.Main;
import scribtest.AllTest;

/**
 * Runs all tests under good and bad root directories in Scribble.
 */
//@RunWith(value = Parameterized.class)
@RunWith(Parameterized.class)
public class LinMPAllTest extends AllTest
{
	private static int NUM_SKIPPED = 0;  // HACK
	
	public LinMPAllTest(String example, boolean isBadTest)
	{
		super(example, isBadTest);
	}

	@Override
	@Test
	public void tests() throws IOException, InterruptedException, ExecutionException
	{
		try
		{
			String dir = ClassLoader.getSystemResource(LinMPAllTest.ALL_ROOT).getFile();

			if (File.separator.equals("\\")) // HACK: Windows
			{
				dir = dir.substring(1).replace("/", "\\");
			}
			
			String[] SKIP =  // HACK
				{
					// "Trying to update class definition for MP3 with incompatible definition..."
					"modules/cli/target/test-classes/good/efsm/gdo/Test04.scr",
					"modules/cli/target/test-classes/good/efsm/gdo/Test08a.scr",
					"modules/cli/target/test-classes/good/efsm/grecursion/choiceunguarded/Test07.scr",
					"modules/cli/target/test-classes/good/misc/globals/gchoice/Choice03.scr",
					"modules/cli/target/test-classes/good/misc/globals/gchoice/Choice04.scr",
					"modules/cli/target/test-classes/good/misc/globals/gchoice/Choice05a.scr",
					"modules/cli/target/test-classes/good/misc/globals/gchoice/Choice05b.scr",
					"modules/cli/target/test-classes/good/misc/globals/gchoice/Choice05c.scr",
					"modules/cli/target/test-classes/good/misc/globals/gdo/Do14.scr",
					"modules/cli/target/test-classes/good/reach/globals/gdo/Test06a.scr",
					"modules/cli/target/test-classes/good/reach/globals/gdo/Test06b.scr",
					"modules/cli/target/test-classes/good/syntax/inlinedunfolding/grecursion/Test01.scr",
					"modules/cli/target/test-classes/good/syntax/inlinedunfolding/grecursion/Test02b.scr",
					"modules/cli/target/test-classes/good/syntax/inlinedunfolding/grecursion/Test02c.scr",
					"modules/cli/target/test-classes/good/wfchoice/gdo/Test01.scr",
					"modules/cli/target/test-classes/good/wfchoice/merge/Test02.scr",

					/*"modules/cli/target/test-classes/bad/wfchoice/enabling/twoparty/Test01b.scr",  // f17 doesn't check choice subjects
					"modules/cli/target/test-classes/bad/wfchoice/gchoice/Choice02.scr",
					//"modules/cli/target/test-classes/bad/wfchoice/enabling/fourparty/Test01.scr"  // The original choice subject problem is gone, but we get a role-progress error instead (without fairness)*/
				};
			String foo = this.example.replace("\\", "/");
			for (String skip : SKIP)
			{
				if (foo.endsWith(skip))
				{
					LinMPAllTest.NUM_SKIPPED++;
					System.out.println("[f17] Manually skipping: " + this.example + " (" + LinMPAllTest.NUM_SKIPPED + " skipped.)");
					return;
				}
			}
			
			/*new CommandLine(this.example, CommandLineArgParser.JUNIT_FLAG, CommandLineArgParser.IMPORT_PATH_FLAG, dir, 
						CommandLineArgParser.F17_FLAG, "[F17AllTest]")  // HACK (cf. F17Main)
					.run();*/
			Main.main(new String[] { "-junit", this.example });
			Assert.assertFalse("Expecting exception", this.isBadTest);
		}
		catch (LinMPSyntaxException e)  // HACK
		{
			LinMPAllTest.NUM_SKIPPED++;
			System.out.println("[f17] Skipping: " + this.example + "  (" + LinMPAllTest.NUM_SKIPPED + " skipped)");
		}
		catch (ScribbleException e)
		{
			Assert.assertTrue("Unexpected exception '" + e.getMessage() + "'", this.isBadTest);
		}
		catch (ScribParserException e)
		{
			throw new RuntimeException(e);
		}
	}
}
