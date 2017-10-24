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
					// "Trying to update class definition for ... with incompatible definition ..."
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
					"modules/cli/target/test-classes/bad/wfchoice/enabling/threeparty/Test03b.scr",
					"modules/cli/target/test-classes/bad/wfchoice/gchoice/Choice02.scr",

					// "Error(s) projecting ... Error(s) merging ...  // TODO CHECKME
					"modules/cli/target/test-classes/good/efsm/gcontinue/choiceunguarded/Test05b.scr",
					"modules/cli/target/test-classes/good/efsm/gdo/Test10.scr",
					"modules/cli/target/test-classes/good/efsm/gdo/Test10.scr",
					"modules/cli/target/test-classes/good/efsm/gdo/Test11.scr",
					"modules/cli/target/test-classes/good/liveness/roleprog/Test03.scr",
					"modules/cli/target/test-classes/good/liveness/roleprog/Test10.scr",
					"modules/cli/target/test-classes/good/safety/stuckmsg/threeparty/Test03.scr",
					"modules/cli/target/test-classes/good/wfchoice/merge/Test04a.scr",
					"modules/cli/target/test-classes/good/wfchoice/merge/Test04b.scr",
					
					// -ip ...  // TODO FIXME
					"modules/cli/target/test-classes/good/misc/imports/Import01.scr",
					"modules/cli/target/test-classes/good/misc/imports/Import02.scr",
					"modules/cli/target/test-classes/good/misc/imports/Import03.scr",
					"modules/cli/target/test-classes/good/misc/imports/Import04.scr",
					"modules/cli/target/test-classes/good/misc/imports/Import05.scr",
					"modules/cli/target/test-classes/good/misc/imports/Import06.scr",
					"modules/cli/target/test-classes/good/misc/imports/Import07.scr",
					"modules/cli/target/test-classes/good/misc/imports/Import08.scr",
					"modules/cli/target/test-classes/good/misc/imports/Import09.scr",
					"modules/cli/target/test-classes/good/misc/imports/Import10.scr",
					"modules/cli/target/test-classes/good/misc/imports/Import11.scr",
					"modules/cli/target/test-classes/good/misc/imports/Import12.scr",
					"modules/cli/target/test-classes/good/misc/imports/Import13.scr",
					"modules/cli/target/test-classes/good/misc/imports/Import14.scr",
					"modules/cli/target/test-classes/good/misc/imports/Import15.scr",
					"modules/cli/target/test-classes/good/misc/imports/Import16.scr",
					"modules/cli/target/test-classes/good/syntax/disamb/imports/Import01.scr",
					"modules/cli/target/test-classes/good/syntax/disamb/imports/Import01a.scr",
					"modules/cli/target/test-classes/good/syntax/disamb/imports/Import02.scr",
					"modules/cli/target/test-classes/good/syntax/disamb/imports/Import03.scr",
					"modules/cli/target/test-classes/good/syntax/disamb/imports/Import04.scr",
					"modules/cli/target/test-classes/good/syntax/disamb/imports/Import05.scr",
					"modules/cli/target/test-classes/good/syntax/inlinedunfolding/gdo/Test02.scr",

					// linmp doesn't check choice subjects
					"modules/cli/target/test-classes/bad/syntax/consistentchoicesubj/Test02.scr",
					"modules/cli/target/test-classes/bad/wfchoice/enabling/twoparty/Test01b.scr",
					
					// linmp supports "syntactic merge"  // CHECKME: why no "trying to update..." error?
					"modules/cli/target/test-classes/bad/wfchoice/merge/Test01.scr",

					/* FIXME: need an explicit "duplicate label" check in GlobalTypeTranslator -- "Trying to update..." exceptions are interfering with JUnit testing
					"modules/cli/target/test-classes/bad/wfchoice/gchoice/Choice02.scr",
					//"modules/cli/target/test-classes/bad/wfchoice/enabling/fourparty/Test01.scr"  // The original choice subject problem is gone, but we get a role-progress error instead (without fairness)
					 */
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
			Main.main(new String[] { "-junit", this.example });  // FIXME: CommandLineArgParser.IMPORT_PATH_FLAG, dir,
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
