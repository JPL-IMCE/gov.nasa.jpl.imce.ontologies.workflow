package gov.nasa.jpl.imce.ontologies.workflow.runner

import gov.nasa.jpl.imce.profileGenerator.batch.tests.RunProfileGenerator

object ProfileGeneratorAsTestExecutor {

   def main(args: Array[String]) {
      println("Starting Profile Generator as Unit Test...")

      (new RunProfileGenerator()).execute()
   }

}
