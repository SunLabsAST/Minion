#!/usr/bin/perl
#
# Turns email into a nice simple format for indexing so that we can
# test field operations.

use Mail::Box::Mbox;
my $folder = Mail::Box::Mbox->new(folder => $ARGV[0]);
foreach $msg ($folder->messages) {
  print "From ", $msg->head->get('from'), "\n";
  print "Subject: ", $msg->head->get('subject'), "\n";
  print "Date: ", $msg->head->get('date'), "\n";
  print "References: ", $msg->head->get('references'), "\n";
  $qb = $msg->decoded;
  $qb =~ s:\nFrom :\n>From :mg;
  print "\n", $qb, "\n";
}
