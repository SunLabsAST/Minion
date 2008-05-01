#!/usr/bin/perl -w

#
# File: convert-reuters.pl
# Created by: Jeff Alexander (ja151348)
# Created on: Tue Mar  1, 2005	 3:06:30 PM
# Desc: 
#

require 5.004;
use strict;

my $Usage = "Usage: $0 <input files ...>\n One output file is created per input file in the current directory\n named after the input file, but with .kt appended to the name\n";

die $Usage unless $#ARGV >= 0;

my $sep = "::";
my $insep= ";;";
my $currFile = "";

for $currFile (@ARGV)
  {
	my $outFile = "$currFile.kt";
	open(INFILE, $currFile);
	open(OUTFILE, ">$outFile");

	my $headerLine;
	my $bodyLine = "";
	my $inBody = 0; # false to start
	my $textLine = "";
	my $inText = 0;
	my $dateline = "";
	my $inDateline = 0;
	my $title = "";
	my $inTitle = 0;
	my $author = "";

	while (<INFILE>)
	  {
		chomp; # eat the newline
		if (/^<REUTERS/)
		  {
			s/<REUTERS//g;
			s/>//g;
			s/TOPICS/HADTOPICS/g;
			s/\"//g;
			s/\s/$sep/g;
			$headerLine = "$_$sep";
			# also reset some variables for the new article
		  }
		elsif (/^<\/REUTERS>/)
		  {
			# reached the end of an article.  dump it
			print OUTFILE "$headerLine\n$bodyLine\n";
			$headerLine = "";
			$bodyLine = "";
		  }
		elsif (/^<DATE>/)
		  {
			s/<[\/]?DATE>//g;
			s/^\s+//;
			$headerLine = $headerLine . "DATE=$_$sep";
		  }
		elsif (/^<TOPICS>/)
		  {
			s/<[\/]?TOPICS>//g;
			s/<D>//g;
			s/<\/D>$//;
			my @items = split(/<\/D>/);
			if ($#items >= 0)
			  {
				$headerLine = $headerLine . "TOPICS=";
				for my $curritem (@items)
				  {
					$headerLine = $headerLine . "$curritem$insep";
				  }
				chop($headerLine);
				chop($headerLine);
				$headerLine = $headerLine . $sep;
			  }
		  }
		elsif (/^<PLACES>/)
		  {
			s/<[\/]?PLACES>//g;
			s/<D>//g;
			s/<\/D>$//;
			my @items = split(/<\/D>/);
			if ($#items >= 0)
			  {
				$headerLine = $headerLine . "PLACES=";
				for my $curritem (@items)
				  {
					$headerLine = $headerLine . "$curritem$insep";
				  }
				chop($headerLine);
				chop($headerLine);
				$headerLine = $headerLine . $sep;
			  }
		  }
		elsif (/^<PEOPLE>/)
		  {
			s/<[\/]?PEOPLE>//g;
			s/<D>//g;
			s/<\/D>$//;
			my @items = split(/<\/D>/);
			if ($#items >= 0)
			  {
				$headerLine = $headerLine . "PEOPLE=";
				for my $curritem (@items)
				  {
					$headerLine = $headerLine . "$curritem$insep";
				  }
				chop($headerLine);
				chop($headerLine);
				$headerLine = $headerLine . $sep;
			  }
		  }
		elsif (/^<ORGS>/)
		  {
			s/<[\/]?ORGS>//g;
			s/<D>//g;
			s/<\/D>$//;
			my @items = split(/<\/D>/);
			if ($#items >= 0)
			  {
				$headerLine = $headerLine . "ORGS=";
				for my $curritem (@items)
				  {
					$headerLine = $headerLine . "$curritem$insep";
				  }
				chop($headerLine);
				chop($headerLine);
				$headerLine = $headerLine . $sep;
			  }
		  }
		elsif (/^<EXCHANGES>/)
		  {
			s/<[\/]?EXCHANGES>//g;
			s/<D>//g;
			s/<\/D>$//;
			my @items = split(/<\/D>/);
			if ($#items >= 0)
			  {
				$headerLine = $headerLine . "EXCHANGES=";
				for my $curritem (@items)
				  {
					$headerLine = $headerLine . "$curritem$insep";
				  }
				chop($headerLine);
				chop($headerLine);
				$headerLine = $headerLine . $sep;
			  }
		  }
		elsif (/^<TEXT/)
		  {
			my (@textTag) = split(/[<>]/);
			my $textAttr = $textTag[1];
			$textAttr =~ s/TEXT//;
			$textAttr =~ s/TYPE/TEXTTYPE/;
			$textAttr =~ s/\"//g;
			$headerLine = $headerLine . "$textAttr$sep";
			$inText = 1;
		  }

		if ($inText)
		  {
			my @line; # we'll use this below
			# Check if this is a title
			if (/<TITLE>/ && /<\/TITLE>/)
			  {
				@line = split(/TITLE/);
				$title = $line[1];
				$title =~ s/[><\/]//g;
				$headerLine = $headerLine . "TITLE=$title$sep";
			  }
			elsif (/<TITLE>/)
			  {
				$inTitle = 1;
				@line = split(/<TITLE>/);
				$title = $title . $line[1];
			  }
			elsif (/<\/TITLE>/)
			  {
				$inTitle = 0;
				@line = split(/<\/TITLE>/);
				$title = $title . $line[0];
				$headerLine = $headerLine . "TITLE=$title$sep";
			  }
			elsif ($inTitle)
			  {
				$title = $title . $_;
			  }

			# do the same thing for dateline
			if (/<DATELINE>/ && /<\/DATELINE>/)
			  {
				@line = split(/DATELINE/);
				$dateline = $line[1];
				$dateline =~ s/[><\/]//g;
				$dateline =~ s/^\s*(.*)\s*$/$1/;
				$headerLine = $headerLine . "DATELINE=$dateline$sep";
			  }
			elsif (/<DATELINE>/)
			  {
				$inDateline = 1;
				@line = split(/<DATELINE>/);
				$dateline = $dateline . $line[1];				
			  }
			elsif (/<\/DATELINE>/)
			  {
				$inDateline = 0;
				@line = split(/<\/DATELINE>/);
				$dateline = $dateline . $line[0];
				$dateline =~ s/^\s*(.*)\s*$/$1/;
				$headerLine = $headerLine . "DATELINE=$dateline$sep";
			  }
			elsif ($inDateline)
			  {
				$dateline = $dateline . $_;
			  }

			# and get the author - always on one line, by itself
			if (/<AUTHOR>/)
			  {
				s/<AUTHOR>//;
				s/<\/AUTHOR>//;
				s/^\s*(.*)\s*$/$1/;
				$headerLine = $headerLine . "AUTHOR=$_$sep";
			  }

			# finally, parse the body
			if (/<BODY>/ && /<\/BODY>/)
			  {
				@line = split(/BODY/);
				$bodyLine = $line[1];
				$bodyLine =~ s/[><\/]//g;
				$bodyLine =~ s/^\s*(.*)\s*$/$1/;
				$bodyLine = "BODY=$bodyLine";				
			  }
			elsif (/<BODY>/)
			  {
				$inBody = 1;
				@line = split(/<BODY>/);
				$bodyLine = $bodyLine . "$line[1] ";				
			  }
			elsif (/<\/BODY>/)
			  {
				$inBody = 0;
				@line = split(/<\/BODY>/);
				$bodyLine = $bodyLine . $line[0];
				$bodyLine =~ s/^\s*(.*)\s*$/$1/;
				$bodyLine = "BODY=$bodyLine";
			  }
			elsif ($inBody)
			  {
				$bodyLine = $bodyLine . "$_ ";
			  }
			
		  }

		if (/<\/TEXT>/)
		  {
			$inText = 0;
			if ($bodyLine eq "")
			  {
				$bodyLine = $title;
			  }
		  }
	  }
  }
